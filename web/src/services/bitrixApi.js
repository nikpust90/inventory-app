/**
 * API клиент для работы с Bitrix24
 * Предоставляет методы для отправки комментариев в заказы
 */
import axios from 'axios';

const BASE_URL = process.env.REACT_APP_BITRIX_BASE_URL || 'https://<bitrix-host>/rest/<user>/<token>/';

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});

/**
 * Отправка комментария в Bitrix24
 * @param {number} bitrixId - ID документа в Bitrix
 * @param {number} typeId - Тип документа (133 - сервисный заказ, 144 - обычный заказ)
 * @param {string} messageText - Текст сообщения
 * @returns {Promise<number>} ID созданного комментария
 */
export const sendComment = async (bitrixId, typeId, messageText) => {
  try {
    const entityType = `dynamic_${typeId}`;
    
    const request = {
      fields: {
        ENTITY_ID: bitrixId,
        ENTITY_TYPE: entityType,
        COMMENT: messageText
      }
    };

    const response = await apiClient.post('crm.timeline.comment.add', request);
    return response.data.result; // ID комментария
  } catch (error) {
    console.error('Ошибка отправки комментария в Bitrix:', error);
    throw error;
  }
};

/**
 * Отправка сообщения напрямую по ID документа
 * @param {string} documentIdString - ID документа (строка)
 * @param {string} messageText - Текст сообщения
 * @param {boolean} isServiceOrder - true для сервисного заказа (133), false для обычного (144)
 * @returns {Promise<number>} ID созданного комментария
 */
export const sendDirectMessage = async (documentIdString, messageText, isServiceOrder) => {
  try {
    const bitrixId = parseInt(documentIdString, 10);
    if (isNaN(bitrixId)) {
      throw new Error('ID должен быть числом');
    }

    const typeId = isServiceOrder ? 133 : 144;
    return await sendComment(bitrixId, typeId, messageText);
  } catch (error) {
    console.error('Ошибка отправки сообщения в Bitrix:', error);
    throw error;
  }
};
