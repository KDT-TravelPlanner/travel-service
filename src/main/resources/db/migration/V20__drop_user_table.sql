-- MSA 전환: travel은 더 이상 User 엔티티를 갖지 않는다. owner/member는 Identity 사용자 UUID만
-- 보관하고, 닉네임·프로필 표시는 Identity HTTP API로 가져온다
-- (SERVICE_COMMUNICATION_BOUNDARIES.md 3). owner_id/user_id 컬럼(UUID)은 그대로 두고
-- user_table로 향하던 FK와 테이블만 제거한다.
ALTER TABLE planners_table DROP CONSTRAINT fk_planners_table_owner;
ALTER TABLE planner_members DROP CONSTRAINT fk_planner_members_user;

DROP TABLE user_table;
