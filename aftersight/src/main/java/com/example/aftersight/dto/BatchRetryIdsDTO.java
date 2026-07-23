package com.example.aftersight.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchRetryIdsDTO {
    private List<Long> ids;
}
