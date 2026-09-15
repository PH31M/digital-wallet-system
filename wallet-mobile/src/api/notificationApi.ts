import { apiClient } from './client';
import { ApiResponse, NotificationResponse, PageResponse } from '../types/api';

export type NotificationsParams = {
  unreadOnly?: boolean;
  page?: number;
  size?: number;
};

export async function getNotificationsApi(
  params: NotificationsParams = {},
): Promise<PageResponse<NotificationResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<NotificationResponse>>>('/notifications', {
    params,
  });
  return response.data.data;
}

export async function getUnreadNotificationCountApi(): Promise<number> {
  const response = await apiClient.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
  return response.data.data.unreadCount;
}

export async function markNotificationReadApi(publicId: string): Promise<NotificationResponse> {
  const response = await apiClient.patch<ApiResponse<NotificationResponse>>(`/notifications/${publicId}/read`);
  return response.data.data;
}

export async function markAllNotificationsReadApi(): Promise<number> {
  const response = await apiClient.patch<ApiResponse<{ updatedCount: number }>>('/notifications/read-all');
  return response.data.data.updatedCount;
}
