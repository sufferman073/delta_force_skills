package com.rzy.dealt_force_skills.ai.operative;

/** Small reactive tree: conditions are rechecked on every decision, including running actions. */
@FunctionalInterface
public interface BehaviorNode {
    enum Status { SUCCESS, FAILURE, RUNNING }

    Status tick();

    static BehaviorNode sequence(BehaviorNode... children) {
        return () -> {
            for (BehaviorNode child : children) {
                Status status = child.tick();
                if (status != Status.SUCCESS) return status;
            }
            return Status.SUCCESS;
        };
    }

    static BehaviorNode selector(BehaviorNode... children) {
        return () -> {
            for (BehaviorNode child : children) {
                Status status = child.tick();
                if (status != Status.FAILURE) return status;
            }
            return Status.FAILURE;
        };
    }
}
