export type Urgency = 'INSTANT' | 'CAN_WAIT';

export interface Gateway {
  id: number;
  code: string;
  name: string;
  fixedFee: number;
  percentageFee: number;
  dailyLimit: number;
  minTransaction: number;
  maxTransaction?: number;
  processingTimeMinutes: number;
  available24x7: boolean;
  availableDays?: string;
  availableFromHour?: number;
  availableToHour?: number;
  active: boolean;
}

export interface RecommendRequest {
  billerId: string;
  amount: number;
  urgency: Urgency;
}

export interface RecommendedGateway {
  id: string;
  name: string;
  estimatedCommission: number;
  processingTime: string;
}

export interface RecommendResponse {
  recommendedGateway: RecommendedGateway;
  alternatives: RecommendedGateway[];
}

export interface GatewayBreakdown {
  gatewayCode: string;
  gatewayName: string;
  transactionCount: number;
  totalAmount: number;
  totalCommission: number;
  dailyLimit: number;
  quotaUsed: number;
  quotaRemaining: number;
}

export interface TransactionView {
  id: number;
  billerCode: string;
  gatewayCode: string;
  gatewayName: string;
  amount: number;
  commission: number;
  totalCharged: number;
  status: string;
  urgency: Urgency;
  splitGroupId?: string;
  createdAt: string;
}

export interface TransactionHistory {
  billerCode: string;
  date: string;
  totalAmount: number;
  totalCommission: number;
  totalTransactions: number;
  breakdown: GatewayBreakdown[];
  transactions: TransactionView[];
}

export interface SplitResponse {
  selectedGateway: string;
  gatewayName: string;
  requiresSplitting: boolean;
  splits: number[];
  totalCommission: number;
  quotaAvailable: boolean;
  splitCount: number;
  splitGroupId?: string;
}
