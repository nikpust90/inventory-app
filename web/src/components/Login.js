/**
 * Компонент авторизации по PIN-коду
 * Поддерживает PIN из 5 символов (цифры и буквы)
 */
import React, { useState, useEffect, useRef } from 'react';
import { loginByPin } from '../services/authApi';
import './Login.css';

/**
 * Компонент Login - экран авторизации
 * @param {Function} onLoginSuccess - Callback при успешной авторизации
 */
const Login = ({ onLoginSuccess }) => {
  const [pin, setPin] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [attempts, setAttempts] = useState(0);
  const [isBlocked, setIsBlocked] = useState(false);
  const inputRef = useRef(null);

  const MAX_ATTEMPTS = 3;

  useEffect(() => {
    // Восстанавливаем счетчик попыток из localStorage
    const savedAttempts = parseInt(localStorage.getItem('loginAttempts') || '0', 10);
    if (savedAttempts >= MAX_ATTEMPTS) {
      setIsBlocked(true);
      setAttempts(MAX_ATTEMPTS);
    } else {
      setAttempts(savedAttempts);
    }

    // Фокус на поле ввода при монтировании
    if (inputRef.current && !isBlocked) {
      inputRef.current.focus();
    }
  }, []);

  /**
   * Обработка изменения PIN
   * Разрешает только цифры и буквы (латиница и кириллица), максимум 5 символов
   */
  const handlePinChange = (e) => {
    const value = e.target.value;
    // Разрешаем только буквы и цифры, максимум 5 символов
    const filtered = value.replace(/[^a-zA-Zа-яА-Я0-9]/g, '').slice(0, 5);
    setPin(filtered);
    setError(null);
  };

  /**
   * Обработка отправки формы
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Проверка на блокировку
    if (isBlocked) {
      setError('Превышено количество попыток входа. Обновите страницу для повторной попытки.');
      return;
    }
    
    if (pin.length < 5) {
      setError('PIN-код должен содержать 5 символов');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const loginData = await loginByPin(pin);
      
      // Успешная авторизация - сбрасываем счетчик попыток
      setAttempts(0);
      localStorage.removeItem('loginAttempts');
      
      // Сохраняем данные в localStorage
      if (loginData.userName) {
        localStorage.setItem('userName', loginData.userName);
      }
      if (loginData.warehouseIds) {
        localStorage.setItem('warehouseIds', JSON.stringify(loginData.warehouseIds));
      }
      if (loginData.admin !== undefined) {
        localStorage.setItem('isAdmin', loginData.admin.toString());
      }

      // Вызываем callback успешной авторизации
      if (onLoginSuccess) {
        onLoginSuccess(loginData);
      }
    } catch (err) {
      // Увеличиваем счетчик попыток
      const newAttempts = attempts + 1;
      setAttempts(newAttempts);
      localStorage.setItem('loginAttempts', newAttempts.toString());
      
      // Проверяем, не превышен ли лимит
      if (newAttempts >= MAX_ATTEMPTS) {
        setIsBlocked(true);
        setError(`Превышено количество попыток входа (${MAX_ATTEMPTS}). Обновите страницу для повторной попытки.`);
        setPin(''); // Очищаем поле
      } else {
        const remaining = MAX_ATTEMPTS - newAttempts;
        setError(`${err.message || 'Ошибка авторизации'} (осталось попыток: ${remaining})`);
        setPin(''); // Очищаем поле при ошибке
        if (inputRef.current) {
          inputRef.current.focus();
        }
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Обработка нажатия клавиш
   */
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && pin.length === 5 && !loading) {
      handleSubmit(e);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">Введите PIN-код</h1>
        <p className="login-subtitle">5 символов (цифры и буквы)</p>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="login-input-container">
            <input
              ref={inputRef}
              type="text"
              className="login-input"
              value={pin}
              onChange={handlePinChange}
              onKeyPress={handleKeyPress}
              placeholder="Введите PIN"
              maxLength={5}
              autoComplete="off"
              disabled={loading || isBlocked}
              autoFocus={!isBlocked}
            />
            <div className="login-pin-indicator">
              {[0, 1, 2, 3, 4].map((index) => (
                <span
                  key={index}
                  className={`pin-dot ${index < pin.length ? 'filled' : ''}`}
                />
              ))}
            </div>
          </div>

          {error && (
            <div className="login-error">
              {error}
            </div>
          )}

          {attempts > 0 && attempts < MAX_ATTEMPTS && (
            <div className="login-attempts-warning">
              Попыток использовано: {attempts} из {MAX_ATTEMPTS}
            </div>
          )}

          <button
            type="submit"
            className="login-button"
            disabled={loading || pin.length !== 5 || isBlocked}
          >
            {loading ? (
              <>
                <span className="spinner-small"></span>
                <span>Вход...</span>
              </>
            ) : isBlocked ? (
              'Вход заблокирован'
            ) : (
              'Войти'
            )}
          </button>

          {isBlocked && (
            <button
              type="button"
              className="login-reset-button"
              onClick={() => {
                setAttempts(0);
                setIsBlocked(false);
                setPin('');
                setError(null);
                localStorage.removeItem('loginAttempts');
                if (inputRef.current) {
                  inputRef.current.focus();
                }
              }}
            >
              Сбросить и попробовать снова
            </button>
          )}
        </form>
      </div>
    </div>
  );
};

export default Login;
