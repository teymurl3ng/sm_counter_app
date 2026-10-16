-- Cover only the new follow-up actor FKs flagged by the DEV advisor.
CREATE INDEX followup_actions_actor ON followup_private.actions(actor_id);
CREATE INDEX followup_claims_actor ON followup_private.claims(actor_id);
CREATE INDEX followup_commands_actor ON followup_private.commands(actor_id);
CREATE INDEX followup_digests_actor ON followup_private.digests(actor_id);
CREATE INDEX followup_policies_primary_actor ON followup_private.policies(primary_actor);
CREATE INDEX followup_policies_updated_by ON followup_private.policies(updated_by);
