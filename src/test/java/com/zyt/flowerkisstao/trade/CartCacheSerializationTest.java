package com.zyt.flowerkisstao.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.flowerkisstao.trade.web.vo.CartVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartCacheSerializationTest {

    @Test
    void cartViewCanRoundTripThroughJsonCache() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CartVO source = CartVO.builder()
                .items(List.of())
                .totalCount(2)
                .totalAmount(new BigDecimal("19.90"))
                .hasInvalid(false)
                .build();

        CartVO restored = mapper.readValue(mapper.writeValueAsString(source), CartVO.class);

        assertEquals(source, restored);
    }
}
