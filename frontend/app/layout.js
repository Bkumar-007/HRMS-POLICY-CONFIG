import "./globals.css";

export const metadata = {
  title: "Recoil HR — Policy Assistant",
  description: "HR policy assistant with RAG, citations, and confirm-before-write actions",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}