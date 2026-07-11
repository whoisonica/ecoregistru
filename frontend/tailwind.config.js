/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // EcoRegistru brand: emerald.
        brand: {
          DEFAULT: "#047857",
          fg: "#ffffff",
          muted: "#ecfdf5",
        },
      },
    },
  },
  plugins: [],
};
