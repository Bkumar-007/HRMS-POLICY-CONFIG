"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction, loginAction } from "@store/auth/authActions";

export default function LoginScreen() {
  const router = useRouter();
  const [username, setUsername] = useState("priya.employee");
  const [password, setPassword] = useState("password");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const session = getSessionAction();
    if (session?.token) {
      router.replace("/chat");
    }
  }, [router]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      await loginAction(username.trim(), password);
      router.replace("/chat");
    } catch (err) {
      setError(err.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page page-login">
      <form className="card login-card" onSubmit={handleSubmit}>
        <p className="brand">Acme HR Assistant</p>
        <h1>Sign in</h1>
        <p className="muted">Demo: priya.employee / password</p>

        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        {error ? <p className="error">{error}</p> : null}

        <button type="submit" disabled={loading}>
          {loading ? "Signing in..." : "Sign in"}
        </button>
      </form>
    </div>
  );
}
