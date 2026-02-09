/**
 * API клиент для работы с остатками на складах
 * Предоставляет методы для получения отчетов по остаткам
 */
import axios from 'axios';

// Используем относительный путь - nginx будет проксировать на backend
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
 * Получить отчет по остаткам на складах
 * @param {string|Array} warehouseIds - Список ID складов через запятую или массив
 * @returns {Promise<Array<StockReport>>} Массив отчетов по складам
 */
export const getStockReport = async (warehouseIds) => {
  try {
    if (!warehouseIds || (typeof warehouseIds === 'string' && warehouseIds.trim() === '')) {
      throw new Error('Не указаны ID складов');
    }

    // Преобразуем в строку, если передан массив
    const warehouseIdsString = Array.isArray(warehouseIds) 
      ? warehouseIds.join(',') 
      : warehouseIds;

    // Если складов слишком много, разбиваем запрос на части
    const MAX_WAREHOUSES_PER_REQUEST = 50; // Максимум складов в одном запросе
    const warehouseArray = warehouseIdsString.split(',').map(id => id.trim()).filter(id => id);
    
    if (warehouseArray.length <= MAX_WAREHOUSES_PER_REQUEST) {
      // Если складов немного, делаем один запрос
      const response = await apiClient.get('/inventory_documents/stock_report', {
        params: { warehouse: warehouseIdsString }
      });
      return response.data || [];
    } else {
      // Если складов много, разбиваем на несколько запросов
      console.log(`Разбиваем запрос на части: ${warehouseArray.length} складов`);
      const allResults = [];
      
      for (let i = 0; i < warehouseArray.length; i += MAX_WAREHOUSES_PER_REQUEST) {
        const chunk = warehouseArray.slice(i, i + MAX_WAREHOUSES_PER_REQUEST);
        const chunkString = chunk.join(',');
        
        console.log(`Запрос части ${Math.floor(i / MAX_WAREHOUSES_PER_REQUEST) + 1}: ${chunk.length} складов`);
        
        const response = await apiClient.get('/inventory_documents/stock_report', {
          params: { warehouse: chunkString }
        });
        
        if (response.data && Array.isArray(response.data)) {
          allResults.push(...response.data);
        }
      }
      
      return allResults;
    }
  } catch (error) {
    console.error('Ошибка загрузки отчета по остаткам:', error);
    
    // Обработка ошибок
    if (error.response) {
      const status = error.response.status;
      if (status === 414) {
        throw new Error('Слишком много складов в запросе. Попробуйте выбрать меньше складов.');
      } else if (status === 404) {
        throw new Error('Отчет не найден');
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
