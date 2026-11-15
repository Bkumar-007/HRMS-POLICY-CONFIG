"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSessionAction } from "@store/auth/authActions";
import { canAccessPath, homePathForRole } from "@configuration/roleAccess";

/**
 * Ensures the user is logged in and allowed to open `path`.
 * Returns session once authorized; otherwise redirects.
 */
export default function useRoleGuard(path) {
  const router = useRouter();
  const [session, setSession] = useState(null);

  useEffect(() => {
    const current = getSessionAction();
    if (!current?.token) {
      router.replace("/login");
      return;
    }
    if (!canAccessPath(current.role, path)) {
      router.replace(homePathForRole(current.role));
      return;
    }
    setSession(current);
  }, [router, path]);

  return session;
}
