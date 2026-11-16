"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  forgotPasswordAction,
  getSessionAction,
  loginAction,
  resetPasswordAction,
  verifyOtpAction,
} from "@store/auth/authActions";
import Button from "@component/common/Button";
import Icon from "@component/common/Icon";
import PasswordField from "@component/common/PasswordField";

const MODE_LOGIN = "login";
const MODE_RESET = "reset";

export default function LoginScreen() {
  const router = useRouter();
  const [mode, setMode] = useState(MODE_LOGIN);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [loading, setLoading] = useState(false);

  const [resetStep, setResetStep] = useState(1);
  const [channel, setChannel] = useState("EMAIL");
  const [maskedDestination, setMaskedDestination] = useState("");
  const [otp, setOtp] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    const session = getSessionAction();
    if (session?.token) {
      router.replace("/chat");
    }
  }, [router]);

  function switchToLogin() {
    setMode(MODE_LOGIN);
    setError("");
    setInfo("");
    setResetStep(1);
    setOtp("");
    setResetToken("");
    setNewPassword("");
    setConfirmPassword("");
  }

  function switchToReset() {
    setMode(MODE_RESET);
    setError("");
    setInfo("");
    setResetStep(1);
    setOtp("");
    setResetToken("");
    setNewPassword("");
    setConfirmPassword("");
  }

  async function handleLogin(event) {
    event.preventDefault();
    setError("");
    setInfo("");
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

  async function handleSendOtp(event) {
    event.preventDefault();
    setError("");
    setInfo("");
    setLoading(true);
    try {
      const result = await forgotPasswordAction(username.trim(), channel);
      setMaskedDestination(result.maskedDestination || "");
      setOtp("");
      setInfo(result.message || "Verification code sent.");
      setResetStep(2);
    } catch (err) {
      setError(err.message || "Could not send verification code");
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyOtp(event) {
    event.preventDefault();
    setError("");
    setInfo("");
    setLoading(true);
    try {
      const result = await verifyOtpAction(username.trim(), otp.trim());
      setResetToken(result.resetToken || "");
      setInfo(result.message || "Code verified.");
      setResetStep(3);
    } catch (err) {
      setError(err.message || "Invalid verification code");
    } finally {
      setLoading(false);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();
    setError("");
    setInfo("");
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    if (newPassword.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    setLoading(true);
    try {
      const result = await resetPasswordAction(username.trim(), resetToken, newPassword);
      setPassword(newPassword);
      setInfo(result.message || "Password updated. Sign in with your new password.");
      setMode(MODE_LOGIN);
      setResetStep(1);
      setOtp("");
      setResetToken("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setError(err.message || "Could not reset password");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page page-login">
      <div className="card login-shell">
        <section className="login-hero">
          <div>
            <p className="brand" style={{ color: "#9fe0cf" }}>
              Recoil HR Portal
            </p>
            <h1>Your workplace, one place</h1>
            <p style={{ margin: 0, color: "rgba(244,248,252,0.82)" }}>
              Ask policy questions, check leave, and manage the HR knowledge library with the same
              security model as a production employee portal.
            </p>
          </div>
          <ul className="login-hero-points">
            <li>
              <Icon name="lock" size={16} />
              Secure sign-in with password visibility control
            </li>
            <li>
              <Icon name="mail" size={16} />
              Forgot password via email or SMS OTP
            </li>
            <li>
              <Icon name="policies" size={16} />
              Policy library with citations in assistant answers
            </li>
          </ul>
        </section>

        <section className="login-panel">
          <div className="login-tabs" role="tablist">
            <button
              type="button"
              className={`login-tab ${mode === MODE_LOGIN ? "is-active" : ""}`}
              onClick={switchToLogin}
            >
              Sign in
            </button>
            <button
              type="button"
              className={`login-tab ${mode === MODE_RESET ? "is-active" : ""}`}
              onClick={switchToReset}
            >
              Forgot password
            </button>
          </div>

          {mode === MODE_LOGIN ? (
            <form onSubmit={handleLogin} style={{ display: "grid", gap: 14 }}>
              <div>
                <h2>Welcome back</h2>
                <p className="muted">
                  Demo: priya.employee · neha.manager · admin.hr / password
                </p>
              </div>

              <label className="field">
                <span className="field-label">Username</span>
                <input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  required
                />
              </label>

              <PasswordField
                label="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />

              <div className="login-meta-row">
                <Button variant="link" type="button" onClick={switchToReset}>
                  Forgot password?
                </Button>
              </div>

              {error ? <p className="error">{error}</p> : null}
              {info ? <p className="success">{info}</p> : null}

              <Button type="submit" icon="lock" loading={loading} fullWidth>
                {loading ? "Signing in..." : "Sign in"}
              </Button>
            </form>
          ) : (
            <div style={{ display: "grid", gap: 14 }}>
              <div>
                <h2>Reset password</h2>
                <p className="muted">
                  We will send a one-time code to the mobile or email on your employee profile.
                </p>
              </div>

              <ol className="reset-steps" aria-label="Reset progress">
                <li className={resetStep >= 1 ? (resetStep === 1 ? "is-current" : "is-done") : ""} />
                <li className={resetStep >= 2 ? (resetStep === 2 ? "is-current" : "is-done") : ""} />
                <li className={resetStep >= 3 ? (resetStep === 3 ? "is-current" : "is-done") : ""} />
              </ol>

              {resetStep === 1 ? (
                <form onSubmit={handleSendOtp} style={{ display: "grid", gap: 14 }}>
                  <label className="field">
                    <span className="field-label">Username</span>
                    <input
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      autoComplete="username"
                      required
                    />
                  </label>

                  <div className="field">
                    <span className="field-label">Send code via</span>
                    <div className="otp-channels">
                      <label className={`otp-channel ${channel === "EMAIL" ? "is-selected" : ""}`}>
                        <input
                          type="radio"
                          name="channel"
                          checked={channel === "EMAIL"}
                          onChange={() => setChannel("EMAIL")}
                        />
                        <Icon name="mail" size={16} />
                        <span>
                          <strong>Email</strong>
                          <p className="otp-hint">OTP to registered email</p>
                        </span>
                      </label>
                      <label className={`otp-channel ${channel === "SMS" ? "is-selected" : ""}`}>
                        <input
                          type="radio"
                          name="channel"
                          checked={channel === "SMS"}
                          onChange={() => setChannel("SMS")}
                        />
                        <Icon name="phone" size={16} />
                        <span>
                          <strong>SMS</strong>
                          <p className="otp-hint">OTP to registered mobile</p>
                        </span>
                      </label>
                    </div>
                  </div>

                  {error ? <p className="error">{error}</p> : null}
                  {info ? <p className="success">{info}</p> : null}

                  <Button type="submit" icon="send" loading={loading} fullWidth>
                    {loading ? "Sending..." : "Send OTP"}
                  </Button>
                </form>
              ) : null}

              {resetStep === 2 ? (
                <form onSubmit={handleVerifyOtp} style={{ display: "grid", gap: 14 }}>
                  <p className="otp-hint">
                    Enter the 6-digit code sent to {maskedDestination || "your registered contact"}.
                  </p>
                  <label className="field">
                    <span className="field-label">Verification code</span>
                    <input
                      value={otp}
                      onChange={(e) => setOtp(e.target.value)}
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      maxLength={8}
                      required
                    />
                  </label>

                  {error ? <p className="error">{error}</p> : null}
                  {info ? <p className="success">{info}</p> : null}

                  <div className="login-meta-row">
                    <Button variant="link" type="button" onClick={() => setResetStep(1)}>
                      Change channel
                    </Button>
                    <Button
                      variant="link"
                      type="button"
                      disabled={loading}
                      onClick={(e) => {
                        e.preventDefault();
                        handleSendOtp(e);
                      }}
                    >
                      Resend code
                    </Button>
                  </div>

                  <Button type="submit" icon="check" loading={loading} fullWidth>
                    {loading ? "Verifying..." : "Verify OTP"}
                  </Button>
                </form>
              ) : null}

              {resetStep === 3 ? (
                <form onSubmit={handleResetPassword} style={{ display: "grid", gap: 14 }}>
                  <PasswordField
                    id="new-password"
                    label="New password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    autoComplete="new-password"
                    required
                  />
                  <PasswordField
                    id="confirm-password"
                    label="Confirm password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    autoComplete="new-password"
                    required
                  />

                  {error ? <p className="error">{error}</p> : null}
                  {info ? <p className="success">{info}</p> : null}

                  <Button type="submit" icon="lock" loading={loading} fullWidth>
                    {loading ? "Updating..." : "Update password"}
                  </Button>
                </form>
              ) : null}

              <Button variant="link" type="button" onClick={switchToLogin}>
                Back to sign in
              </Button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
