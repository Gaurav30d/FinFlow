import React from 'react';

/**
 * Hand-drawn, tasteful doodle decorations for FinFlow.
 * Pure SVG, theme-adaptive, accessible (aria-hidden, pointer-events-none).
 */

interface DoodleProps {
  className?: string;
  size?: number;
}

/** Organic 4-point hand-sketched sparkle */
export const DoodleSparkle: React.FC<DoodleProps> = ({ className = 'text-amber-500/60 dark:text-amber-400/50', size = 24 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={`pointer-events-none select-none ${className}`}
    aria-hidden="true"
  >
    <path
      d="M12 2C12.3 6.8 15.2 9.7 20 10C15.2 10.3 12.3 13.2 12 18C11.7 13.2 8.8 10.3 4 10C8.8 9.7 11.7 6.8 12 2Z"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

/** Playful hand-drawn 5-point star */
export const DoodleStar: React.FC<DoodleProps> = ({ className = 'text-blue-500/50 dark:text-blue-400/40', size = 20 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={`pointer-events-none select-none ${className}`}
    aria-hidden="true"
  >
    <path
      d="M12 2.5L14.6 8.5L21 9.1L16.2 13.4L17.6 19.8L12 16.5L6.4 19.8L7.8 13.4L3 9.1L9.4 8.5L12 2.5Z"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

/** Hand-drawn curved directional arrow pointing toward actions or balances */
export const DoodleArrow: React.FC<{
  className?: string;
  direction?: 'down-right' | 'up-right' | 'curved-left' | 'curved-right';
  width?: number;
  height?: number;
}> = ({ className = 'text-blue-600/50 dark:text-blue-400/40', direction = 'down-right', width = 48, height = 36 }) => {
  return (
    <svg
      width={width}
      height={height}
      viewBox="0 0 60 45"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`pointer-events-none select-none ${className}`}
      aria-hidden="true"
    >
      {direction === 'down-right' ? (
        <>
          <path
            d="M5 8C18 6 38 12 45 28C47 32 48 35 48 38"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
          />
          <path
            d="M38 34C43 36 47.5 38 48 38.5C48.5 38 49 32 50 26"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      ) : direction === 'up-right' ? (
        <>
          <path
            d="M8 38C15 28 32 15 48 10"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
          />
          <path
            d="M36 8C43 9 48 10 48.5 10C48 14 46 22 45 26"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      ) : (
        <>
          <path
            d="M50 10C35 8 18 18 12 32"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
          />
          <path
            d="M10 22C11 28 12 32 12.5 32C16 31 23 28 26 26"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      )}
    </svg>
  );
};

/** Organic, hand-sketched underline/wave under key words */
export const DoodleUnderline: React.FC<{ className?: string; width?: number; height?: number }> = ({
  className = 'text-blue-500/60 dark:text-blue-400/50',
  width = 160,
  height = 14,
}) => (
  <svg
    width={width}
    height={height}
    viewBox="0 0 160 14"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={`pointer-events-none select-none ${className}`}
    aria-hidden="true"
  >
    <path
      d="M2 9C30 3 65 11 95 6C120 2 142 9 158 5"
      stroke="currentColor"
      strokeWidth="2.4"
      strokeLinecap="round"
    />
  </svg>
);

/** Loose, sketched highlight loop/circle around a word or badge */
export const DoodleCircle: React.FC<{ className?: string; width?: number; height?: number }> = ({
  className = 'text-indigo-500/40 dark:text-indigo-400/35',
  width = 90,
  height = 42,
}) => (
  <svg
    width={width}
    height={height}
    viewBox="0 0 90 42"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={`pointer-events-none select-none ${className}`}
    aria-hidden="true"
  >
    <path
      d="M45 4C65 3.5 86 10 86 21C86 31 65 38 43 38C20 38 4 30 4 20C4 10 25 3 50 4"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
    />
  </svg>
);

/** Floating sketched coin accent */
export const DoodleCoin: React.FC<DoodleProps> = ({ className = 'text-amber-500/60 dark:text-amber-400/50', size = 28 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 28 28"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={`pointer-events-none select-none ${className}`}
    aria-hidden="true"
  >
    <circle cx="14" cy="14" r="11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    <ellipse cx="14" cy="14" rx="8" ry="8" stroke="currentColor" strokeWidth="1.2" strokeDasharray="3 2" />
    <path d="M12 9.5H16M14 9.5V18.5M11.5 13.5H16.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
);

/** Hand-drawn upward growth trajectory */
export const DoodleGrowth: React.FC<DoodleProps> = ({ className = 'text-emerald-500/60 dark:text-emerald-400/50', size = 32 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={`pointer-events-none select-none ${className}`}
    aria-hidden="true"
  >
    <path d="M4 26L11 19L17 23L27 9" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M20 9H27V16" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

/** Smooth floating pill badge (e.g., instant transfer, AI insights) */
export const FloatingCardBadge: React.FC<{
  title: string;
  subtitle?: string;
  icon?: React.ReactNode;
  className?: string;
  delayClass?: string;
}> = ({ title, subtitle, icon, className = '', delayClass = 'animate-float-slow' }) => (
  <div
    className={`inline-flex items-center gap-2.5 px-3.5 py-2 rounded-2xl bg-white/80 dark:bg-neutral-900/80 backdrop-blur-md border border-neutral-200/80 dark:border-neutral-800/80 shadow-md shadow-neutral-900/5 select-none pointer-events-none ${delayClass} ${className}`}
  >
    {icon && <div className="shrink-0">{icon}</div>}
    <div className="text-left">
      <div className="text-xs font-bold text-neutral-800 dark:text-neutral-100">{title}</div>
      {subtitle && <div className="text-[10px] text-neutral-500 dark:text-neutral-400">{subtitle}</div>}
    </div>
  </div>
);
