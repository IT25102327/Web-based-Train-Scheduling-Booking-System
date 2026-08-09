/**
 * @file server.js
 * @description Express server entry point. Connects to the MySQL database via Sequelize and starts listening for HTTP requests.
 */

const app = require('./app');
const { sequelize } = require('./config/db.config');
const { PORT } = require('./config/app.config');

const startServer = async () => {
  try {
    await sequelize.authenticate();
    console.log('✅ Database connection established successfully.');
  } catch (error) {
    console.warn('⚠️  MySQL not connected — server will start without database.');
    console.warn('   Reason:', error.message);
    console.warn('   Start MySQL and restart the server when ready.\n');
  }

  app.listen(PORT, () => {
    console.log(`🚀 Backend server listening on http://localhost:${PORT}`);
  });
};

startServer();
