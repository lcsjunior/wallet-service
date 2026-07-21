package com.example.wallet.mapper;

import com.example.wallet.dto.BalanceResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

  @Mapping(target = "walletId", source = "id")
  WalletResponse toWalletResponse(Wallet wallet);

  @Mapping(target = "walletId", source = "id")
  BalanceResponse toBalanceResponse(Wallet wallet);
}
