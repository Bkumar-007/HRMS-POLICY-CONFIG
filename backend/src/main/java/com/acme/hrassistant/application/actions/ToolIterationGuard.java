package com.acme.hrassistant.application.actions;

/**
 * Per-request counter for tool calls. Enforces max iterations on the agent loop from Java
 * (Spring AI 1.1 does not expose a maxToolCalls option).
 */
public class ToolIterationGuard {

    private final int maxIterations;
    private int count;

    public ToolIterationGuard(int maxIterations) {
        this.maxIterations = Math.max(1, maxIterations);
    }

    public void enter(String toolName) {
        count++;
        if (count > maxIterations) {
            throw new IllegalStateException(
                    "Tool iteration limit exceeded (" + maxIterations + ") while calling " + toolName);
        }
    }

    public int getCount() {
        return count;
    }
}
