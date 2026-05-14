/** @type {import('tailwindcss').Config} */
export default {
    content: [
        './vendor/laravel/framework/src/Illuminate/Pagination/resources/views/*.blade.php',
        './storage/framework/views/*.php',
        './resources/views/**/*.blade.php',
        './resources/js/**/*.jsx',
    ],
    theme: {
        extend: {
            colors: {
                glass: {
                    bg: 'rgba(255, 255, 255, 0.05)',
                    border: 'rgba(255, 255, 255, 0.12)',
                    hover: 'rgba(255, 255, 255, 0.08)',
                },
                dark: {
                    950: '#0A0A0F',
                    900: '#0F0F1A',
                    800: '#1A1A2E',
                    700: '#16213E',
                    600: '#0F3460',
                },
                brand: {
                    purple: '#7C3AED',
                    'purple-light': '#A78BFA',
                    'purple-dark': '#5B21B6',
                    cyan: '#06B6D4',
                    'cyan-light': '#67E8F9',
                    pink: '#EC4899',
                }
            },
            backgroundImage: {
                'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
                'hero-glow': 'radial-gradient(ellipse 80% 50% at 50% -20%, rgba(120, 58, 237, 0.3), transparent)',
                'card-glow': 'linear-gradient(135deg, rgba(124, 58, 237, 0.1), rgba(6, 182, 212, 0.05))',
            },
            backdropBlur: {
                xs: '2px',
                glass: '12px',
                heavy: '24px',
            },
            boxShadow: {
                glass: '0 8px 32px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255,255,255,0.1)',
                'glass-sm': '0 4px 16px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255,255,255,0.08)',
                'glow-purple': '0 0 20px rgba(124, 58, 237, 0.4), 0 0 60px rgba(124, 58, 237, 0.1)',
                'glow-cyan': '0 0 20px rgba(6, 182, 212, 0.4)',
                'glow-pink': '0 0 20px rgba(236, 72, 153, 0.4)',
            },
            animation: {
                'float': 'float 6s ease-in-out infinite',
                'pulse-glow': 'pulseGlow 2s ease-in-out infinite',
                'shimmer': 'shimmer 2s linear infinite',
            },
            keyframes: {
                float: {
                    '0%, 100%': { transform: 'translateY(0)' },
                    '50%': { transform: 'translateY(-10px)' },
                },
                pulseGlow: {
                    '0%, 100%': { opacity: '1' },
                    '50%': { opacity: '0.5' },
                },
                shimmer: {
                    '0%': { backgroundPosition: '-1000px 0' },
                    '100%': { backgroundPosition: '1000px 0' },
                },
            },
        },
    },
    plugins: [],
};
