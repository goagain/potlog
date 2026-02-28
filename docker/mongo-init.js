db = db.getSiblingDB('potlog');

db.createCollection('sessions');

db.sessions.createIndex({ numericId: 1 }, { unique: true });
db.sessions.createIndex({ status: 1 });
db.sessions.createIndex({ 'players.userId': 1 });
db.sessions.createIndex({ createdAt: -1 });

print('MongoDB initialized with potlog database and indexes');
