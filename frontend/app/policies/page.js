import { Suspense } from "react";
import PoliciesScreen from "@component/policies/PoliciesScreen";

export default function PoliciesPage() {
  return (
    <Suspense fallback={<div className="page">Loading policies…</div>}>
      <PoliciesScreen />
    </Suspense>
  );
}
