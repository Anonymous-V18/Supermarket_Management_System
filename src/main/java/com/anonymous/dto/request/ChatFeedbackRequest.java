package com.anonymous.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatFeedbackRequest {

    // 1 helpful, -1 not helpful, 0 clears the vote
    @NotNull(message = "Vui lòng chọn đánh giá")
    @Min(value = -1, message = "Đánh giá không hợp lệ")
    @Max(value = 1, message = "Đánh giá không hợp lệ")
    Integer value;

}
