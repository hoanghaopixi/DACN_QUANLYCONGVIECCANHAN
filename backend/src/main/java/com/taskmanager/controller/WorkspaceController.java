package com.taskmanager.controller;

import com.taskmanager.entity.Category;
import com.taskmanager.entity.Workspace;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.WorkspaceRepository;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserResolver userResolver;

    @GetMapping
    public ResponseEntity<List<Workspace>> getWorkspaces(HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        List<Workspace> list = workspaceRepository.findByUserIdOrderByIdAsc(userId);

        // Khởi tạo các Không gian làm việc mặc định vào CSDL nếu người dùng chưa có
        if (list.isEmpty()) {
            Workspace ws1 = Workspace.builder()
                    .userId(userId)
                    .name("Không gian cá nhân")
                    .isDefault(true)
                    .iconName("User")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            Workspace savedWs = workspaceRepository.save(ws1);

            // Tạo các danh mục mặc định cho user ở không gian này
            Category cat1 = Category.builder().userId(userId).workspaceId(savedWs.getId()).name("Công việc").colorCode("#8B5CF6").iconName("Briefcase").isDefault(true).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
            Category cat2 = Category.builder().userId(userId).workspaceId(savedWs.getId()).name("Học tập").colorCode("#3B82F6").iconName("GraduationCap").isDefault(false).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
            Category cat3 = Category.builder().userId(userId).workspaceId(savedWs.getId()).name("Sức khỏe").colorCode("#10B981").iconName("Heart").isDefault(false).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
            Category cat4 = Category.builder().userId(userId).workspaceId(savedWs.getId()).name("Giải trí").colorCode("#F59E0B").iconName("Smile").isDefault(false).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
            categoryRepository.saveAll(List.of(cat1, cat2, cat3, cat4));

            list = workspaceRepository.findByUserIdOrderByIdAsc(userId);
        }

        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Workspace> createWorkspace(@RequestBody Workspace workspace, HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        workspace.setUserId(userId);
        if (workspace.getName() == null || workspace.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        workspace.setName(workspace.getName().trim());
        workspace.setCreatedAt(OffsetDateTime.now());
        workspace.setUpdatedAt(OffsetDateTime.now());
        return ResponseEntity.ok(workspaceRepository.save(workspace));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workspace> updateWorkspace(
            @PathVariable Long id,
            @RequestBody Workspace dto,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        Workspace workspace = workspaceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new com.taskmanager.exception.ResourceNotFoundException("Không tìm thấy không gian làm việc #" + id));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            workspace.setName(dto.getName().trim());
        }
        workspace.setUpdatedAt(OffsetDateTime.now());
        return ResponseEntity.ok(workspaceRepository.save(workspace));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteWorkspace(@PathVariable Long id, HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        List<Workspace> list = workspaceRepository.findByUserIdOrderByIdAsc(userId);

        if (list.size() <= 1) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Bạn phải giữ lại ít nhất một không gian làm việc trong cơ sở dữ liệu!"
            ));
        }

        Workspace ws = workspaceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new com.taskmanager.exception.ResourceNotFoundException("Không tìm thấy không gian làm việc #" + id));

        // Ràng buộc kiểm tra xóa:
        long taskCount = taskRepository.countByUserIdAndWorkspaceId(userId, id);
        if (taskCount > 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Không thể xóa không gian làm việc \"" + ws.getName() + "\" vì đang có " + taskCount + " công việc đang thuộc không gian này!",
                "taskCount", taskCount
            ));
        }

        // Xóa các nhóm rỗng thuộc workspace này
        categoryRepository.deleteByWorkspaceIdAndUserId(id, userId);

        workspaceRepository.delete(ws);
        return ResponseEntity.noContent().build();
    }
}
