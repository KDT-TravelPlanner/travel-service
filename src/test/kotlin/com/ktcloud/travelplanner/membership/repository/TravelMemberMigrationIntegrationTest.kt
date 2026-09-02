package com.ktcloud.travelplanner.membership.repository

import com.ktcloud.travelplanner.testsupport.ContainerIntegrationTestSupport
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

class TravelMemberMigrationIntegrationTest : ContainerIntegrationTestSupport() {
	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Autowired
	private lateinit var dataSource: DataSource

	@Test
	fun `current planner schema upgrades through membership migration`() {
		val schema = "membership_upgrade_test"
		jdbcTemplate.execute("DROP SCHEMA IF EXISTS $schema CASCADE")
		jdbcTemplate.execute("CREATE SCHEMA $schema")

		try {
			Flyway.configure()
				.dataSource(dataSource)
				.schemas(schema)
				.defaultSchema(schema)
				.target(MigrationVersion.fromVersion("4"))
				.load()
				.migrate()

			val result = Flyway.configure()
				.dataSource(dataSource)
				.schemas(schema)
				.defaultSchema(schema)
				.target(MigrationVersion.fromVersion("5"))
				.load()
				.migrate()

			assertEquals(1, result.migrationsExecuted)
			assertEquals(
				1,
				jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM $schema.flyway_schema_history WHERE version = '5' AND success = TRUE",
					Int::class.java,
				),
			)
		} finally {
			jdbcTemplate.execute("DROP SCHEMA IF EXISTS $schema CASCADE")
		}
	}

	@Test
	fun `role status and unique membership constraints reject invalid rows`() {
		val ownerId = UUID.randomUUID()
		val inviteeId = UUID.randomUUID()
		val travelId = UUID.randomUUID()
		val now = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
		// V20 이후 owner_id/user_id는 Identity 사용자 UUID일 뿐 user_table FK가 없다.
		jdbcTemplate.update(
			"INSERT INTO planners_table " +
				"(id, owner_id, title, start_date, end_date, created_at, updated_at) " +
				"VALUES (?, ?, '멤버십 여행', '2026-08-01', '2026-08-02', ?, ?)",
			travelId,
			ownerId,
			now,
			now,
		)

		assertThrows<DataIntegrityViolationException> {
			insertMember(travelId, inviteeId, "OWNER", "PENDING", now)
		}
		insertMember(travelId, inviteeId, "READ_ONLY", "PENDING", now)
		// 같은 (planner_id, user_id) 재삽입은 UNIQUE 제약 위반
		assertThrows<DataIntegrityViolationException> {
			insertMember(travelId, inviteeId, "READ_WRITE", "PENDING", now)
		}
	}

	private fun insertMember(
		travelId: UUID,
		userId: UUID,
		role: String,
		status: String,
		now: OffsetDateTime,
	) {
		jdbcTemplate.update(
			"INSERT INTO planner_members (id, planner_id, user_id, role, status, invited_at) " +
				"VALUES (?, ?, ?, ?, ?, ?)",
			UUID.randomUUID(),
			travelId,
			userId,
			role,
			status,
			now,
		)
	}
}
