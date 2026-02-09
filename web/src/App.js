/**
 * Главный компонент приложения
 * Отображает компонент для работы со схемами автомобилей и остатками
 * С авторизацией по PIN-коду
 */
import React, { useState, useEffect } from 'react';
import Login from './components/Login';
import CarScheme from './components/CarScheme';
import StockReport from './components/StockReport';
import './App.css';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentView, setCurrentView] = useState('cars'); // 'cars' или 'stock'
  const [userData, setUserData] = useState(null);

  // Проверяем авторизацию при загрузке
  useEffect(() => {
    const userName = localStorage.getItem('userName');
    if (userName) {
      setIsAuthenticated(true);
      const warehouseIds = JSON.parse(localStorage.getItem('warehouseIds') || '[]');
      const isAdmin = localStorage.getItem('isAdmin') === 'true';
      setUserData({
        userName,
        warehouseIds,
        isAdmin
      });
    }
  }, []);

  /**
   * Обработка успешной авторизации
   */
  const handleLoginSuccess = (loginData) => {
    setUserData(loginData);
    setIsAuthenticated(true);
  };

  /**
   * Выход из системы
   */
  const handleLogout = () => {
    localStorage.removeItem('userName');
    localStorage.removeItem('warehouseIds');
    localStorage.removeItem('isAdmin');
    setIsAuthenticated(false);
    setUserData(null);
  };

  // Если не авторизован, показываем экран входа
  if (!isAuthenticated) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  // Основное приложение
  return (
    <div className="App">
      <nav className="app-nav">
        <div className="app-nav-left">
          <button 
            className={`nav-button ${currentView === 'cars' ? 'active' : ''}`}
            onClick={() => setCurrentView('cars')}
          >
            Схемы автомобилей
          </button>
          <button 
            className={`nav-button ${currentView === 'stock' ? 'active' : ''}`}
            onClick={() => setCurrentView('stock')}
          >
            Остатки на складах
          </button>
        </div>
        <div className="app-nav-right">
          <span className="user-name">{userData?.userName}</span>
          <button className="logout-button" onClick={handleLogout}>
            Выйти
          </button>
        </div>
      </nav>

      {currentView === 'cars' && <CarScheme />}
      {currentView === 'stock' && (
        <StockReport warehouseIds={userData?.warehouseIds || []} />
      )}
    </div>
  );
}

export default App;
