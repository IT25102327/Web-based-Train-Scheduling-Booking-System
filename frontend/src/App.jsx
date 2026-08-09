/**
 * App Root Component
 * Wraps top-level context providers (Auth, Theme) and mounts application routes.
 */
import React from 'react';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import AppRoutes from './routes/AppRoutes';

function App() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <div className="app-container">
          <AppRoutes />
        </div>
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;
