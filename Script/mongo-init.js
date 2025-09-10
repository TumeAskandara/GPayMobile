// mongo-init.js - MongoDB initialization script
// Place this in scripts/mongo-init.js

// Create the gpay_db database and collections
db = db.getSiblingDB('gpay_db');

// Create collections with validation
db.createCollection('users', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['email', 'phoneNumber', 'createdAt'],
            properties: {
                email: {
                    bsonType: 'string',
                    pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
                    description: 'must be a valid email'
                },
                phoneNumber: {
                    bsonType: 'string',
                    pattern: '^[+]?[1-9]\\d{1,14}$',
                    description: 'must be a valid phone number'
                },
                firstName: {
                    bsonType: 'string',
                    minLength: 1,
                    maxLength: 50
                },
                lastName: {
                    bsonType: 'string',
                    minLength: 1,
                    maxLength: 50
                },
                status: {
                    enum: ['ACTIVE', 'INACTIVE', 'SUSPENDED'],
                    description: 'must be a valid status'
                },
                createdAt: {
                    bsonType: 'date'
                },
                updatedAt: {
                    bsonType: 'date'
                }
            }
        }
    }
});

db.createCollection('transactions', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['userId', 'amount', 'type', 'status', 'createdAt'],
            properties: {
                userId: {
                    bsonType: 'objectId'
                },
                amount: {
                    bsonType: 'decimal',
                    minimum: 0
                },
                type: {
                    enum: ['CREDIT', 'DEBIT', 'TRANSFER'],
                    description: 'must be a valid transaction type'
                },
                status: {
                    enum: ['PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'],
                    description: 'must be a valid status'
                },
                description: {
                    bsonType: 'string',
                    maxLength: 255
                },
                createdAt: {
                    bsonType: 'date'
                }
            }
        }
    }
});

db.createCollection('wallets', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['userId', 'balance', 'currency', 'createdAt'],
            properties: {
                userId: {
                    bsonType: 'objectId'
                },
                balance: {
                    bsonType: 'decimal',
                    minimum: 0
                },
                currency: {
                    bsonType: 'string',
                    enum: ['USD', 'EUR', 'INR', 'GBP'],
                    description: 'must be a valid currency code'
                },
                isActive: {
                    bsonType: 'bool'
                },
                createdAt: {
                    bsonType: 'date'
                },
                updatedAt: {
                    bsonType: 'date'
                }
            }
        }
    }
});

db.createCollection('payment_methods', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['userId', 'type', 'isActive', 'createdAt'],
            properties: {
                userId: {
                    bsonType: 'objectId'
                },
                type: {
                    enum: ['CREDIT_CARD', 'DEBIT_CARD', 'BANK_ACCOUNT', 'UPI'],
                    description: 'must be a valid payment method type'
                },
                isActive: {
                    bsonType: 'bool'
                },
                createdAt: {
                    bsonType: 'date'
                }
            }
        }
    }
});

// Create indexes for performance
db.users.createIndex({ 'email': 1 }, { unique: true });
db.users.createIndex({ 'phoneNumber': 1 }, { unique: true });
db.users.createIndex({ 'status': 1 });
db.users.createIndex({ 'createdAt': 1 });

db.transactions.createIndex({ 'userId': 1 });
db.transactions.createIndex({ 'status': 1 });
db.transactions.createIndex({ 'type': 1 });
db.transactions.createIndex({ 'createdAt': -1 });
db.transactions.createIndex({ 'userId': 1, 'createdAt': -1 });

db.wallets.createIndex({ 'userId': 1 }, { unique: true });
db.wallets.createIndex({ 'isActive': 1 });

db.payment_methods.createIndex({ 'userId': 1 });
db.payment_methods.createIndex({ 'isActive': 1 });
db.payment_methods.createIndex({ 'type': 1 });

// Insert sample data for testing
db.users.insertMany([
    {
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        phoneNumber: '+1234567890',
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane.smith@example.com',
        phoneNumber: '+1234567891',
        status: 'ACTIVE',
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

print('MongoDB initialization completed successfully!');
print('Collections created: users, transactions, wallets, payment_methods');
print('Indexes created for optimal performance');
print('Sample data inserted for testing');

