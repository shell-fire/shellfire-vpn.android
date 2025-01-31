package de.shellfire.vpn.android;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

    @Entity(tableName = "obfuscated_account_id")
    public class ObfuscatedAccountId {
        @PrimaryKey(autoGenerate = true)
        private int id;
        private String obfuscatedAccountId;

        // Constructor
        public ObfuscatedAccountId(String obfuscatedAccountId) {
            this.obfuscatedAccountId = obfuscatedAccountId;
        }

        // Getters and setters
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getObfuscatedAccountId() {
            return obfuscatedAccountId;
        }

        public void setObfuscatedAccountId(String obfuscatedAccountId) {
            this.obfuscatedAccountId = obfuscatedAccountId;
        }
    }
