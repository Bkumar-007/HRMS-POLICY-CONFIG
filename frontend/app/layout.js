import "./globals.css";

export const metadata = {
  title: "Recoil HR Portal",
  description: "Employee HR portal — policy assistant and actions",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}