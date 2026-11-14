import "./globals.css";

export const metadata = {
  title: "Acme HR Assistant",
  description: "HR Policy Assistant + Action Agent demo",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}