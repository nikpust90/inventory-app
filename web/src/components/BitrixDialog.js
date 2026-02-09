/**
 * Компонент диалога для отправки сообщения в Bitrix24
 * Позволяет ввести ID заказа и отправить сообщение
 */
import React, { useState } from 'react';
import { sendDirectMessage } from '../services/bitrixApi';
import './BitrixDialog.css';

/**
 * Компонент BitrixDialog - модальное окно для отправки в Bitrix
 * @param {boolean} isOpen - Открыт ли диалог
 * @param {Function} onClose - Функция закрытия диалога
 * @param {string} messageText - Текст сообщения для отправки
 */
const BitrixDialog = ({ isOpen, onClose, messageText }) => {
  const [orderId, setOrderId] = useState('');
  const [isServiceOrder, setIsServiceOrder] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  if (!isOpen) return null;

  /**
   * Обработка отправки сообщения
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!orderId.trim()) {
      setError('Введите номер (ID) заказа');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      await sendDirectMessage(orderId.trim(), messageText, isServiceOrder);
      setSuccess(true);
      
      // Закрываем диалог через 2 секунды после успешной отправки
      setTimeout(() => {
        handleClose();
      }, 2000);
    } catch (err) {
      if (err.message && err.message.includes('числом')) {
        setError('ID должен быть числом!');
      } else if (err.response) {
        setError(`Ошибка отправки: ${err.response.status} ${err.response.statusText}`);
      } else {
        setError('Ошибка отправки сообщения. Проверьте подключение к интернету.');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Закрытие диалога и сброс состояния
   */
  const handleClose = () => {
    setOrderId('');
    setIsServiceOrder(false);
    setError(null);
    setSuccess(false);
    onClose();
  };

  return (
    <div className="bitrix-dialog-overlay" onClick={handleClose}>
      <div className="bitrix-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="bitrix-dialog-header">
          <h2>Отправить схему в Битрикс</h2>
          <button className="bitrix-dialog-close" onClick={handleClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="bitrix-dialog-form">
          <div className="bitrix-dialog-field">
            <label htmlFor="orderId">Номер заказа (ID):</label>
            <input
              id="orderId"
              type="text"
              value={orderId}
              onChange={(e) => setOrderId(e.target.value)}
              placeholder="Введите ID заказа"
              autoFocus
              disabled={loading || success}
            />
          </div>

          <div className="bitrix-dialog-field">
            <label className="bitrix-dialog-checkbox">
              <input
                type="checkbox"
                checked={isServiceOrder}
                onChange={(e) => setIsServiceOrder(e.target.checked)}
                disabled={loading || success}
              />
              <span>Сервисный заказ</span>
            </label>
          </div>

          {error && (
            <div className="bitrix-dialog-error">
              {error}
            </div>
          )}

          {success && (
            <div className="bitrix-dialog-success">
              ✅ Успешно отправлено в Битрикс!
            </div>
          )}

          <div className="bitrix-dialog-actions">
            <button
              type="button"
              className="bitrix-dialog-button cancel"
              onClick={handleClose}
              disabled={loading}
            >
              Отмена
            </button>
            <button
              type="submit"
              className="bitrix-dialog-button submit"
              disabled={loading || success || !orderId.trim()}
            >
              {loading ? 'Отправка...' : 'Отправить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BitrixDialog;
