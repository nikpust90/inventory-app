/**
 * API клиент для авторизации по PIN-коду
 * Поддерживает PIN из 5 символов (цифры и буквы)
 */
import axios from 'axios';

// Используем относительный путь - nginx будет проксировать на backend или напрямую к 1С
const BASE_URL = process.env.REACT_APP_API_URL || '/api';

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});

/**
 * Авторизация по PIN-коду
 * @param {string} pin - PIN-код (5 символов: цифры и/или буквы)
 * @returns {Promise<LoginResponse>} Данные пользователя после успешной авторизации
 */
export const loginByPin = async (pin) => {
  try {
    if (!pin || pin.trim() === '') {
      throw new Error('PIN-код не может быть пустым');
    }

    // Логируем запрос для отладки
    console.log('Отправка запроса авторизации:', {
      url: `${BASE_URL}/inventory_documents/login`,
      pin: pin.trim().substring(0, 2) + '***' // Маскируем PIN в логах
    });

    const response = await apiClient.post('/inventory_documents/login', {
      pin: pin.trim()
    });

    console.log('Ответ авторизации:', response.status, response.data);

    return response.data;
  } catch (error) {
    console.error('Ошибка авторизации:', error);
    
    if (error.response) {
      // Обработка ошибок от сервера
      const status = error.response.status;
      if (status === 400) {
        throw new Error('ПИН-код не передан');
      } else if (status === 401) {
        throw new Error('Ошибка авторизации. Проверьте подключение к серверу 1С');
      } else if (status === 404) {
        throw new Error('Пользователь с таким ПИН-кодом не найден');
      } else if (status === 500) {
        throw new Error('Внутренняя ошибка сервера. Попробуйте позже');
      } else {
        throw new Error(error.response.data?.message || `Ошибка сервера: ${status}`);
      }
    } else if (error.request) {
      throw new Error('Ошибка сети. Проверьте подключение к интернету');
    } else {
      throw error;
    }
  }
};
