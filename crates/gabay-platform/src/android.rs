//! Android permission backend over JNI.
//!
//! The Activity object comes from `ndk_context`, which `android-activity`
//! (and therefore winit) initialises before `android_main` runs.
//!
//! Known limits, by design:
//! * Results of `requestPermissions` are not delivered to Rust (that needs a
//!   Java subclass). Callers poll `status`, which is cheap.
//! * `requestPermissions` / `startActivity` are invoked from the native
//!   thread rather than the UI thread. Both work in practice from native
//!   activities; see docs/IMPLEMENTATION.md, "Open risks".

use jni::objects::{JObject, JString, JValue};
use jni::{JNIEnv, JavaVM};

use crate::backend::PermissionBackend;
use crate::error::PlatformError;
use crate::permission::{Grant, Permission};

const REQUEST_CODE: i32 = 0x6761; // "ga"
const SDK_R: i32 = 30;
const SDK_Q: i32 = 29;
const SDK_TIRAMISU: i32 = 33;

const FINE_LOCATION: &str = "android.permission.ACCESS_FINE_LOCATION";
const COARSE_LOCATION: &str = "android.permission.ACCESS_COARSE_LOCATION";
const BACKGROUND_LOCATION: &str = "android.permission.ACCESS_BACKGROUND_LOCATION";
const RECORD_AUDIO: &str = "android.permission.RECORD_AUDIO";
const POST_NOTIFICATIONS: &str = "android.permission.POST_NOTIFICATIONS";
const READ_EXTERNAL_STORAGE: &str = "android.permission.READ_EXTERNAL_STORAGE";
const WRITE_EXTERNAL_STORAGE: &str = "android.permission.WRITE_EXTERNAL_STORAGE";

const ACTION_ALL_FILES_FOR_APP: &str = "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION";
const ACTION_ALL_FILES: &str = "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION";
const ACTION_APP_DETAILS: &str = "android.settings.APPLICATION_DETAILS_SETTINGS";

pub struct AndroidBackend;

impl AndroidBackend {
    pub fn new() -> Self {
        Self
    }
}

impl Default for AndroidBackend {
    fn default() -> Self {
        Self::new()
    }
}

type JniResult<T> = jni::errors::Result<T>;

/// Attaches the current thread and runs `f` with the Activity. Any pending
/// Java exception is cleared so it cannot poison later calls.
fn with_activity<R>(
    f: impl FnOnce(&mut JNIEnv, &JObject) -> JniResult<R>,
) -> Result<R, PlatformError> {
    let ctx = ndk_context::android_context();
    // SAFETY: `ndk_context` guarantees a valid JavaVM pointer for the process lifetime.
    let vm = unsafe { JavaVM::from_raw(ctx.vm().cast()) }
        .map_err(|e| PlatformError::Call(format!("JavaVM: {e}")))?;
    let mut env = vm
        .attach_current_thread()
        .map_err(|e| PlatformError::Call(format!("attach: {e}")))?;
    // SAFETY: the pointer is the Activity global reference owned by android-activity;
    // `JObject` does not delete it on drop.
    let activity = unsafe { JObject::from_raw(ctx.context().cast()) };

    match f(&mut env, &activity) {
        Ok(value) => Ok(value),
        Err(err) => {
            if env.exception_check().unwrap_or(false) {
                let _ = env.exception_clear();
            }
            Err(PlatformError::Call(err.to_string()))
        }
    }
}

fn sdk_int(env: &mut JNIEnv) -> JniResult<i32> {
    env.get_static_field("android/os/Build$VERSION", "SDK_INT", "I")?.i()
}

fn is_granted(env: &mut JNIEnv, activity: &JObject, name: &str) -> JniResult<bool> {
    let jname = env.new_string(name)?;
    let code = env
        .call_method(
            activity,
            "checkSelfPermission",
            "(Ljava/lang/String;)I",
            &[JValue::Object(&jname)],
        )?
        .i()?;
    Ok(code == 0) // PackageManager.PERMISSION_GRANTED
}

fn request_runtime(env: &mut JNIEnv, activity: &JObject, names: &[&str]) -> JniResult<()> {
    let string_class = env.find_class("java/lang/String")?;
    let first = env.new_string(names[0])?;
    let array = env.new_object_array(names.len() as i32, &string_class, &first)?;
    for (i, name) in names.iter().enumerate().skip(1) {
        let item = env.new_string(*name)?;
        env.set_object_array_element(&array, i as i32, &item)?;
    }
    env.call_method(
        activity,
        "requestPermissions",
        "([Ljava/lang/String;I)V",
        &[JValue::Object(&array), JValue::Int(REQUEST_CODE)],
    )?;
    Ok(())
}

fn open_settings(
    env: &mut JNIEnv,
    activity: &JObject,
    action: &str,
    with_package: bool,
) -> JniResult<()> {
    let jaction = env.new_string(action)?;
    let intent = env.new_object(
        "android/content/Intent",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&jaction)],
    )?;
    if with_package {
        let pkg_obj = env
            .call_method(activity, "getPackageName", "()Ljava/lang/String;", &[])?
            .l()?;
        let pkg: String = env.get_string(&JString::from(pkg_obj))?.into();
        let uri_text = env.new_string(format!("package:{pkg}"))?;
        let uri = env
            .call_static_method(
                "android/net/Uri",
                "parse",
                "(Ljava/lang/String;)Landroid/net/Uri;",
                &[JValue::Object(&uri_text)],
            )?
            .l()?;
        env.call_method(
            &intent,
            "setData",
            "(Landroid/net/Uri;)Landroid/content/Intent;",
            &[JValue::Object(&uri)],
        )?;
    }
    env.call_method(
        activity,
        "startActivity",
        "(Landroid/content/Intent;)V",
        &[JValue::Object(&intent)],
    )?;
    Ok(())
}

impl PermissionBackend for AndroidBackend {
    fn status(&self, permission: Permission) -> Result<Grant, PlatformError> {
        with_activity(|env, activity| {
            let sdk = sdk_int(env)?;
            let grant = |ok: bool| if ok { Grant::Granted } else { Grant::Denied };
            Ok(match permission {
                Permission::Location => grant(is_granted(env, activity, FINE_LOCATION)?),
                Permission::Microphone => grant(is_granted(env, activity, RECORD_AUDIO)?),
                Permission::Notifications => {
                    if sdk < SDK_TIRAMISU {
                        Grant::NotApplicable
                    } else {
                        grant(is_granted(env, activity, POST_NOTIFICATIONS)?)
                    }
                }
                Permission::VaultStorage => {
                    if sdk >= SDK_R {
                        let ok = env
                            .call_static_method(
                                "android/os/Environment",
                                "isExternalStorageManager",
                                "()Z",
                                &[],
                            )?
                            .z()?;
                        grant(ok)
                    } else {
                        grant(is_granted(env, activity, READ_EXTERNAL_STORAGE)?)
                    }
                }
                Permission::BackgroundLocation => {
                    if sdk < SDK_Q {
                        Grant::NotApplicable
                    } else {
                        grant(is_granted(env, activity, BACKGROUND_LOCATION)?)
                    }
                }
            })
        })
    }

    fn request(&self, permission: Permission) -> Result<(), PlatformError> {
        with_activity(|env, activity| {
            let sdk = sdk_int(env)?;
            match permission {
                Permission::Location => {
                    request_runtime(env, activity, &[FINE_LOCATION, COARSE_LOCATION])
                }
                Permission::Microphone => request_runtime(env, activity, &[RECORD_AUDIO]),
                Permission::Notifications => {
                    if sdk >= SDK_TIRAMISU {
                        request_runtime(env, activity, &[POST_NOTIFICATIONS])
                    } else {
                        Ok(())
                    }
                }
                Permission::VaultStorage => {
                    if sdk >= SDK_R {
                        // Prefer the per-app screen; fall back to the global list.
                        if open_settings(env, activity, ACTION_ALL_FILES_FOR_APP, true).is_err() {
                            if env.exception_check()? {
                                env.exception_clear()?;
                            }
                            open_settings(env, activity, ACTION_ALL_FILES, false)?;
                        }
                        Ok(())
                    } else {
                        request_runtime(env, activity, &[READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE])
                    }
                }
                Permission::BackgroundLocation => {
                    if sdk >= SDK_R {
                        // Android 11+ only grants this from the app's Settings page.
                        open_settings(env, activity, ACTION_APP_DETAILS, true)
                    } else if sdk == SDK_Q {
                        request_runtime(env, activity, &[BACKGROUND_LOCATION])
                    } else {
                        Ok(())
                    }
                }
            }
        })
    }

    fn open_app_settings(&self) -> Result<(), PlatformError> {
        with_activity(|env, activity| open_settings(env, activity, ACTION_APP_DETAILS, true))
    }
}
