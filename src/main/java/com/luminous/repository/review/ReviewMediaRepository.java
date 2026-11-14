package com.luminous.repository.review;

import com.luminous.model.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Integer> {

    List<ReviewMedia> findByReviewIdOrderByIdAsc(Integer reviewId);

    List<ReviewMedia> findByReviewIdIn(List<Integer> reviewIds);
}
