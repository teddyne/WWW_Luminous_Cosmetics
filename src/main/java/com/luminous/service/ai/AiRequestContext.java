package com.luminous.service.ai;

import com.luminous.dto.ai.AiProductView;
import com.luminous.model.User;
import com.luminous.service.ai.AiOrderDraftService.DraftView;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiRequestContext {

    private final ThreadLocal<User> currentUser = new ThreadLocal<>();
    private final ThreadLocal<HttpSession> session = new ThreadLocal<>();
    private final ThreadLocal<List<AiProductView>> lastProducts = new ThreadLocal<>();
    private final ThreadLocal<DraftView> lastDraft = new ThreadLocal<>();

    public void begin(User user, HttpSession session) {
        currentUser.set(user);
        this.session.set(session);
        lastProducts.remove();
        lastDraft.remove();
    }

    public void end() {
        currentUser.remove();
        session.remove();
        lastProducts.remove();
        lastDraft.remove();
    }

    public User user() {
        return currentUser.get();
    }

    public HttpSession session() {
        return session.get();
    }

    public List<AiProductView> lastProducts() {
        return lastProducts.get() == null ? List.of() : lastProducts.get();
    }

    public void lastProducts(List<AiProductView> products) {
        lastProducts.set(products);
    }

    public DraftView lastDraft() {
        return lastDraft.get();
    }

    public void lastDraft(DraftView draft) {
        lastDraft.set(draft);
    }
}
