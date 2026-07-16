package com.mascode.itineraire.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `local_account` (
                    `id` INTEGER NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `app_security` (
                    `id` INTEGER NOT NULL,
                    `biometricLockEnabled` INTEGER NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `quick_actions` (
                    `id` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `eventType` TEXT NOT NULL,
                    `placeId` TEXT,
                    `notes` TEXT,
                    `position` INTEGER NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    `updatedAt` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`placeId`) REFERENCES `places`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_quick_actions_label` ON `quick_actions` (`label`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_quick_actions_placeId` ON `quick_actions` (`placeId`)",
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `planned_journey_legs` (
                    `id` TEXT NOT NULL,
                    `journeyId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `sourcePlaceId` TEXT NOT NULL,
                    `destinationPlaceId` TEXT NOT NULL,
                    `transportMode` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`journeyId`) REFERENCES `journeys`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`sourcePlaceId`) REFERENCES `places`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(`destinationPlaceId`) REFERENCES `places`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_journey_legs_journeyId_position` " +
                    "ON `planned_journey_legs` (`journeyId`, `position`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planned_journey_legs_sourcePlaceId` " +
                    "ON `planned_journey_legs` (`sourcePlaceId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planned_journey_legs_destinationPlaceId` " +
                    "ON `planned_journey_legs` (`destinationPlaceId`)",
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `journey_legs` ADD COLUMN `costPending` INTEGER NOT NULL DEFAULT 0",
            )
        }
    }
}
