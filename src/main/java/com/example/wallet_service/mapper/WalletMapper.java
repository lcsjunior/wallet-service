package com.example.wallet_service.mapper;

import com.example.wallet_service.dto.WalletResponse;
import com.example.wallet_service.entity.Wallet;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "walletId", source = "id")
    WalletResponse toWalletResponse(Wallet wallet);
}
