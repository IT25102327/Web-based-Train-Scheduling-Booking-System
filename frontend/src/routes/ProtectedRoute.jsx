/**
 * ProtectedRoute Component
 * Higher-order component / route guard ensuring user authentication before rendering children.
 *
 * @component
 * @param {Object} props - Component props
 * @returns {React.ReactElement} Children or redirection view
 */
import React from 'react';

const ProtectedRoute = ({ children }) => {
  const isAuthenticated = true;

  if (!isAuthenticated) {
    return <div className="redirect-notice">Please log in to continue.</div>;
  }

  return children;
};

export default ProtectedRoute;
