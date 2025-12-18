export interface Result<T> {
  code: 0 | 1;
  msg: string;
  data: T;
}

export interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
