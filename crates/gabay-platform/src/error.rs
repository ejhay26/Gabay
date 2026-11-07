use thiserror::Error;

#[derive(Debug, Error)]
pub enum PlatformError {
    #[error("platform call failed: {0}")]
    Call(String),
    #[error("not supported on this platform: {0}")]
    Unsupported(&'static str),
}
