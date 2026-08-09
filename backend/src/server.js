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
    console.log('Database connection established successfully.');

    app.listen(PORT, () => {
      console.log(`Server listening on port ${PORT}`);
    });
  } catch (error) {
    console.error('Unable to connect to the database:', error);
    process.exit(1);
  }
};

startServer();
