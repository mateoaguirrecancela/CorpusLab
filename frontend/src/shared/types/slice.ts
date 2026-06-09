export type SliceResponseDto<T> = {
  content: T[];
  first: boolean;
  last: boolean;
  number: number;
  size: number;
  numberOfElements: number;
  empty: boolean;
};

export type SliceResponse<T> = SliceResponseDto<T>;

export function mapSliceResponse<TDto, TModel>(
  dto: SliceResponseDto<TDto>,
  mapItem: (item: TDto) => TModel,
): SliceResponse<TModel> {
  return {
    ...dto,
    content: dto.content.map(mapItem),
  };
}
