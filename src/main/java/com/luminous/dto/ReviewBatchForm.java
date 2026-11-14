package com.luminous.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

/** Form danh gia ca don 1 lan (kieu Shopee): 1 muc cho moi san pham trong don. */
@Getter
@Setter
public class ReviewBatchForm {

    private List<ReviewItemForm> items = new ArrayList<>();

    @Getter
    @Setter
    public static class ReviewItemForm {

        private Integer detailId;

        /** De trong = bo qua san pham nay (khong bat buoc danh gia het). */
        private Byte rating;

        private String comment;

        /** true = hien ten dang nhap tren danh gia (mac dinh), false = an danh. */
        private boolean showName = true;

        private List<MultipartFile> images;

        private MultipartFile video;
    }
}
