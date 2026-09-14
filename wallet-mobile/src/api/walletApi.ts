import { apiClient } from './client';
import {
  ApiResponse,
  PageResponse,
  TransactionHistoryItemResponse,
  TransactionResponse,
  WalletLookupResponse,
  WalletResponse,
} from '../types/api';

export async function getCurrentUserWalletApi(): Promise<WalletResponse> {
  const response = await apiClient.get<ApiResponse<WalletResponse>>('/wallets/me');
  return response.data.data;
}

export async function lookupWalletApi(identifier: string): Promise<WalletLookupResponse> {
  const response = await apiClient.get<ApiResponse<WalletLookupResponse>>('/wallets/lookup', {
    params: { identifier },
  });
  return response.data.data;
}

export async function getWalletApi(id: string): Promise<WalletResponse> {
  const response = await apiClient.get<ApiResponse<WalletResponse>>(`/wallets/${id}`);
  return response.data.data;
}

export type TransactionHistoryParams = {
  page?: number;
  size?: number;
};

export async function getTransactionHistoryApi(
  params: TransactionHistoryParams = {},
): Promise<PageResponse<TransactionHistoryItemResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<TransactionHistoryItemResponse>>>(
    '/transactions/history',
    { params },
  );
  return response.data.data;
}

export async function depositApi(amount: number, idempotencyKey: string): Promise<TransactionResponse> {
  const response = await apiClient.post<ApiResponse<TransactionResponse>>('/wallets/me/deposits', {
    amount,
    idempotency_key: idempotencyKey,
  });
  return response.data.data;
}

export async function withdrawApi(amount: number, idempotencyKey: string): Promise<TransactionResponse> {
  const response = await apiClient.post<ApiResponse<TransactionResponse>>('/wallets/me/withdrawals', {
    amount,
    idempotency_key: idempotencyKey,
  });
  return response.data.data;
}

export async function transferApi(
  receiverWalletId: string,
  amount: number,
  idempotencyKey: string,
): Promise<TransactionResponse> {
  const response = await apiClient.post<ApiResponse<TransactionResponse>>('/wallets/me/transfers', {
    receiver_wallet_id: receiverWalletId,
    amount,
    idempotency_key: idempotencyKey,
  });
  return response.data.data;
}

export async function confirmTransactionOtpApi(transactionId: string, otp: string): Promise<TransactionResponse> {
  const response = await apiClient.post<ApiResponse<TransactionResponse>>(
    `/transactions/${transactionId}/confirm-otp`,
    { otp },
  );
  return response.data.data;
}
