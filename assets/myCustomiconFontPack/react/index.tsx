import React from 'react';

export interface IconProps extends React.SVGProps<SVGSVGElement> {
  size?: number | string;
  color?: string;
}

export const ScooterIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="6" cy="18" r="3" />
  <circle cx="18" cy="18" r="3" />
  <circle cx="6" cy="18" r="1" fill="currentColor" />
  <circle cx="18" cy="18" r="1" fill="currentColor" />
  <path d="M6 15h4l3-6h4" />
  <path d="M12 12l2 6h3" />
  <path d="M7 12h5" />
  <path d="M17 6l2-3" />
  <path d="M15 6h4" />
  <path d="M17 9l1-3" />
</svg>
);

export const CompassStarIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <polygon points="12 2 15 9 22 12 15 15 12 22 9 15 2 12 9 9" />
  <circle cx="12" cy="12" r="2.5" fill="currentColor" />
  <path d="M12 2v10l3-3" fill="currentColor" opacity="0.3" />
</svg>
);

export const WaypointPinIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 21c-4.5-5-7-8.5-7-12a7 7 0 1 1 14 0c0 3.5-2.5 7-7 12z" />
  <polygon points="12 6 15 11 12 10 9 11" fill="currentColor" />
</svg>
);

export const FuelPumpIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <rect x="4" y="4" width="10" height="16" rx="2" />
  <rect x="6" y="7" width="6" height="4" rx="1" />
  <path d="M4 17h10" />
  <path d="M14 9h2a2 2 0 0 1 2 2v5a1.5 1.5 0 0 0 3 0V9l-2-2" />
  <circle cx="21" cy="7" r="1" fill="currentColor" />
</svg>
);

export const RoadCenterlineIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M4 21L8 3" />
  <path d="M20 21L16 3" />
  <line x1="12" y1="5" x2="12" y2="8" stroke-dasharray="0" />
  <line x1="12" y1="11" x2="12" y2="14" stroke-dasharray="0" />
  <line x1="12" y1="17" x2="12" y2="20" stroke-dasharray="0" />
</svg>
);

export const PerspectiveCubeIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 2l9 5v10l-9 5-9-5V7l9-5z" />
  <line x1="12" y1="12" x2="21" y2="7" />
  <line x1="12" y1="12" x2="12" y2="22" />
  <line x1="12" y1="12" x2="3" y2="7" />
</svg>
);

export const WrenchToolIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
</svg>
);

export const SpeakerWavesIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
  <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
  <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
</svg>
);

export const SpeedometerDialIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 4a9 9 0 0 0-9 9c0 2.5 1 4.7 2.6 6.4L7 18" />
  <path d="M21 13a9 9 0 0 0-9-9" />
  <path d="M17 18l1.4 1.4A9 9 0 0 0 21 13" />
  <line x1="12" y1="13" x2="16" y2="9" />
  <circle cx="12" cy="13" r="1.5" fill="currentColor" />
</svg>
);

export const ShieldCrestIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  <path d="M12 6v12" stroke-dasharray="2 2" opacity="0.6" />
</svg>
);

export const BatteryBoltIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <rect x="2" y="6" width="18" height="12" rx="2" />
  <line x1="22" y1="10" x2="22" y2="14" />
  <polygon points="12 8 8 13 11 13 10 16 14 11 11 11" fill="currentColor" />
</svg>
);

export const GearSettingsIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="3" />
  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
</svg>
);

export const HelmetSafetyIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 3a9 9 0 0 0-9 9c0 3.5 2 6.5 5 8h8a6 6 0 0 0 6-6c0-6.08-4.92-11-10-11z" />
  <path d="M8 12h8a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8" fill="currentColor" opacity="0.25" />
</svg>
);

export const HazardTriangleIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
  <line x1="12" y1="9" x2="12" y2="13" />
  <line x1="12" y1="17" x2="12.01" y2="17" />
</svg>
);

export const GforceMeterIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="12" r="9" />
  <line x1="12" y1="3" x2="12" y2="21" stroke-dasharray="2 2" />
  <line x1="3" y1="12" x2="21" y2="12" stroke-dasharray="2 2" />
  <circle cx="14" cy="10" r="2.5" fill="currentColor" />
</svg>
);

export const SatelliteDishIcon: React.FC<IconProps> = ({ size = 24, color = 'currentColor', ...props }) => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M4 10a7.5 7.5 0 0 0 10 10" />
  <path d="M4 6a11.5 11.5 0 0 0 14 14" />
  <path d="M4 2a15.5 15.5 0 0 0 18 18" />
  <circle cx="4" cy="20" r="2" fill="currentColor" />
</svg>
);
