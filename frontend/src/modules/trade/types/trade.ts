/** 与后端 trade/web/vo/*.java 一一对应 */

/** 订单状态。数值与后端 OrderStatus 枚举一致，改了会与存量数据错位 */
export const OrderStatus = {
  PENDING_PAY: 0,
  PAID: 1,
  SHIPPED: 2,
  COMPLETED: 3,
  CANCELLED: 4,
  AFTER_SALE: 5,
} as const

export type OrderStatusValue = (typeof OrderStatus)[keyof typeof OrderStatus]

/** 与后端 AddressVO 对应 */
export interface Address {
  id: number
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
  /** 省市区 + 详细地址，后端拼好的 */
  fullAddress: string
}

export interface AddressInput {
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
}

/**
 * 与后端 CartItemVO 对应。
 *
 * price 与 stock 都是后端每次实时从 catalog_sku 读的，不是加购时的快照——
 * 前端也绝不缓存它们。
 */
export interface CartItem {
  skuId: number
  speciesId: number | null
  /** 品种 code，跳详情页用 */
  slug: string | null
  speciesName: string
  spec: string | null
  image: string | null
  imageAlt: string | null
  price: number
  quantity: number
  subtotal: number
  stock: number

  /** 已下架或已删除 */
  unavailable: boolean
  /** 库存不足以支撑当前数量 */
  outOfStock: boolean
  /** 失效原因文案，为 null 表示可正常结算 */
  invalidReason: string | null
}

/** 与后端 CartVO 对应。合计只统计可结算的条目 */
export interface Cart {
  items: CartItem[]
  totalCount: number
  totalAmount: number
  hasInvalid: boolean
}

/** 与后端 OrderItemVO 对应，全部字段都是下单时的快照 */
export interface OrderItem {
  id: number
  skuId: number
  speciesId: number
  speciesName: string
  spec: string
  image: string
  /** 成交单价，不是当前售价 */
  unitPrice: number
  quantity: number
  subtotal: number
}

/**
 * 当前角色在这个状态下能做的动作，由后端算好。
 *
 * 前端不再自己判断状态——状态机在后端，前端再实现一遍必然会有出入，
 * 界面上就会出现点下去必然报错的按钮。
 */
export interface OrderActionItem {
  /** PAY / CANCEL / SHIP / RECEIVE / APPLY_AFTER_SALE / APPROVE_AFTER_SALE / REJECT_AFTER_SALE */
  code: string
  label: string
}

/** 与后端 OrderVO 对应 */
export interface Order {
  id: number
  orderNo: string
  status: OrderStatusValue
  statusLabel: string
  totalAmount: number
  itemCount: number

  receiver: string
  phone: string
  addressSnapshot: string

  createdAt: string
  paidAt: string | null
  shippedAt: string | null
  receivedAt: string | null
  closedAt: string | null

  cancelReason: string | null
  refundReason: string | null
  refundRejectReason: string | null

  items: OrderItem[]
  actions: OrderActionItem[]
}
