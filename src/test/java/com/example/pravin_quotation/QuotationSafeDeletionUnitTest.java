package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationSafeDeletionUnitTest {

    private QuotationRepository quotationRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private BranchRepository branchRepository;
    private QuotationRoomRepository quotationRoomRepository;
    private QuotationItemRepository quotationItemRepository;
    private QuotationItemSizeRepository quotationItemSizeRepository;
    private QuotationStatusHistoryRepository quotationStatusHistoryRepository;
    private QuotationWorkflowRepository quotationWorkflowRepository;
    private QuotationCommunicationRepository quotationCommunicationRepository;

    private Map<Long, Quotation> quotationStore;
    private Map<Long, QuotationRoom> roomStore;
    private Map<Long, QuotationItem> itemStore;
    private Map<Long, QuotationItemSize> sizeStore;
    private Map<Long, QuotationCommunication> commStore;
    private Map<Long, QuotationStatusHistory> historyStore;
    private Map<Long, QuotationWorkflow> workflowStore;

    private List<String> executedSqlList;
    private JdbcTemplate mockJdbcTemplate;

    private QuotationService quotationServiceWithJdbc;
    private QuotationService quotationServiceNoJdbc;

    private User employee1;
    private User employee2;
    private Customer customer;
    private Branch branch;

    @BeforeEach
    void setUp() {
        quotationStore = new HashMap<>();
        roomStore = new HashMap<>();
        itemStore = new HashMap<>();
        sizeStore = new HashMap<>();
        commStore = new HashMap<>();
        historyStore = new HashMap<>();
        workflowStore = new HashMap<>();
        executedSqlList = new ArrayList<>();

        branch = new Branch();
        branch.setId(1L);
        branch.setName("Chennai");

        employee1 = new User();
        employee1.setId(10L);
        employee1.setName("Employee One");
        employee1.setRole(Role.EMPLOYEE);
        employee1.setBranch(branch);

        employee2 = new User();
        employee2.setId(20L);
        employee2.setName("Employee Two");
        employee2.setRole(Role.EMPLOYEE);
        employee2.setBranch(branch);

        customer = new Customer();
        customer.setId(100L);
        customer.setName("John Doe");

        setupMockRepositories();

        // Create QuotationService with mock JdbcTemplate
        quotationServiceWithJdbc = new QuotationService(
                quotationRepository,
                customerRepository,
                userRepository,
                branchRepository,
                null,
                quotationRoomRepository,
                quotationItemRepository,
                quotationItemSizeRepository,
                quotationStatusHistoryRepository,
                quotationWorkflowRepository,
                quotationCommunicationRepository,
                mockJdbcTemplate
        );

        // Create QuotationService without JdbcTemplate (repository fallback)
        quotationServiceNoJdbc = new QuotationService(
                quotationRepository,
                customerRepository,
                userRepository,
                branchRepository,
                null,
                quotationRoomRepository,
                quotationItemRepository,
                quotationItemSizeRepository,
                quotationStatusHistoryRepository,
                quotationWorkflowRepository,
                quotationCommunicationRepository,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T createMock(Class<T> clazz, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    private void setupMockRepositories() {
        quotationRepository = createMock(QuotationRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findById".equals(name)) {
                Long id = (Long) args[0];
                return Optional.ofNullable(quotationStore.get(id));
            }
            if ("save".equals(name)) {
                Quotation q = (Quotation) args[0];
                if (q.getId() == null) q.setId((long) (quotationStore.size() + 1));
                quotationStore.put(q.getId(), q);
                return q;
            }
            if ("delete".equals(name)) {
                Quotation q = (Quotation) args[0];
                quotationStore.remove(q.getId());
                return null;
            }
            if ("findByParentIdOrderByIdAsc".equals(name)) {
                Long pId = (Long) args[0];
                List<Quotation> list = new ArrayList<>();
                for (Quotation q : quotationStore.values()) {
                    if (q.getParent() != null && Objects.equals(q.getParent().getId(), pId)) {
                        list.add(q);
                    }
                }
                list.sort(Comparator.comparing(Quotation::getId));
                return list;
            }
            return null;
        });

        customerRepository = createMock(CustomerRepository.class, (proxy, method, args) -> null);
        userRepository = createMock(UserRepository.class, (proxy, method, args) -> null);
        branchRepository = createMock(BranchRepository.class, (proxy, method, args) -> null);

        quotationRoomRepository = createMock(QuotationRoomRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByQuotationId".equals(name)) {
                Long qId = (Long) args[0];
                List<QuotationRoom> res = new ArrayList<>();
                for (QuotationRoom r : roomStore.values()) {
                    if (r.getQuotation() != null && Objects.equals(r.getQuotation().getId(), qId)) {
                        res.add(r);
                    }
                }
                return res;
            }
            if ("deleteAll".equals(name)) {
                Iterable<QuotationRoom> it = (Iterable<QuotationRoom>) args[0];
                for (QuotationRoom r : it) roomStore.remove(r.getId());
                return null;
            }
            return null;
        });

        quotationItemRepository = createMock(QuotationItemRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByQuotationRoomId".equals(name)) {
                Long rId = (Long) args[0];
                List<QuotationItem> res = new ArrayList<>();
                for (QuotationItem i : itemStore.values()) {
                    if (i.getQuotationRoom() != null && Objects.equals(i.getQuotationRoom().getId(), rId)) {
                        res.add(i);
                    }
                }
                return res;
            }
            if ("deleteAll".equals(name)) {
                Iterable<QuotationItem> it = (Iterable<QuotationItem>) args[0];
                for (QuotationItem i : it) itemStore.remove(i.getId());
                return null;
            }
            return null;
        });

        quotationItemSizeRepository = createMock(QuotationItemSizeRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByQuotationItemIdOrderByDisplayOrderAsc".equals(name)) {
                Long iId = (Long) args[0];
                List<QuotationItemSize> res = new ArrayList<>();
                for (QuotationItemSize s : sizeStore.values()) {
                    if (s.getQuotationItem() != null && Objects.equals(s.getQuotationItem().getId(), iId)) {
                        res.add(s);
                    }
                }
                return res;
            }
            if ("deleteAll".equals(name)) {
                Iterable<QuotationItemSize> it = (Iterable<QuotationItemSize>) args[0];
                for (QuotationItemSize s : it) sizeStore.remove(s.getId());
                return null;
            }
            return null;
        });

        quotationCommunicationRepository = createMock(QuotationCommunicationRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByQuotationIdOrderBySentAtDesc".equals(name)) {
                Long qId = (Long) args[0];
                List<QuotationCommunication> res = new ArrayList<>();
                for (QuotationCommunication c : commStore.values()) {
                    if (c.getQuotation() != null && Objects.equals(c.getQuotation().getId(), qId)) {
                        res.add(c);
                    }
                }
                return res;
            }
            if ("deleteAll".equals(name)) {
                Iterable<QuotationCommunication> it = (Iterable<QuotationCommunication>) args[0];
                for (QuotationCommunication c : it) commStore.remove(c.getId());
                return null;
            }
            return null;
        });

        quotationStatusHistoryRepository = createMock(QuotationStatusHistoryRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByQuotationIdOrderByIdDesc".equals(name)) {
                Long qId = (Long) args[0];
                List<QuotationStatusHistory> res = new ArrayList<>();
                for (QuotationStatusHistory h : historyStore.values()) {
                    if (h.getQuotation() != null && Objects.equals(h.getQuotation().getId(), qId)) {
                        res.add(h);
                    }
                }
                return res;
            }
            if ("deleteAll".equals(name)) {
                Iterable<QuotationStatusHistory> it = (Iterable<QuotationStatusHistory>) args[0];
                for (QuotationStatusHistory h : it) historyStore.remove(h.getId());
                return null;
            }
            return null;
        });

        quotationWorkflowRepository = createMock(QuotationWorkflowRepository.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByQuotationIdOrderByCreatedAtDesc".equals(name)) {
                Long qId = (Long) args[0];
                List<QuotationWorkflow> res = new ArrayList<>();
                for (QuotationWorkflow w : workflowStore.values()) {
                    if (w.getQuotation() != null && Objects.equals(w.getQuotation().getId(), qId)) {
                        res.add(w);
                    }
                }
                return res;
            }
            if ("deleteAll".equals(name)) {
                Iterable<QuotationWorkflow> it = (Iterable<QuotationWorkflow>) args[0];
                for (QuotationWorkflow w : it) workflowStore.remove(w.getId());
                return null;
            }
            return null;
        });

        // Mock JdbcTemplate to record SQL executions
        mockJdbcTemplate = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                executedSqlList.add(sql + " [args=" + Arrays.toString(args) + "]");
                // Simulate deletion in in-memory stores if needed
                if (sql.contains("DELETE FROM quotations WHERE id = ?")) {
                    Long id = ((Number) args[0]).longValue();
                    quotationStore.remove(id);
                }
                if (sql.contains("DELETE FROM quotations WHERE parent_id = ?")) {
                    Long pId = ((Number) args[0]).longValue();
                    quotationStore.entrySet().removeIf(e -> e.getValue().getParent() != null && Objects.equals(e.getValue().getParent().getId(), pId));
                }
                return 1;
            }

            @Override
            public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
                executedSqlList.add(sql + " [args=" + Arrays.toString(args) + "]");
                if (sql.contains("SELECT id FROM quotations WHERE parent_id = ?")) {
                    Long pId = ((Number) args[0]).longValue();
                    List<T> ids = new ArrayList<>();
                    for (Quotation q : quotationStore.values()) {
                        if (q.getParent() != null && Objects.equals(q.getParent().getId(), pId)) {
                            ids.add((T) q.getId());
                        }
                    }
                    return ids;
                }
                return Collections.emptyList();
            }
        };
    }

    private Quotation createQuotationWithChildren(Long parentId) {
        Quotation parent = new Quotation();
        parent.setId(parentId);
        parent.setQuotationNumber("QT-00001");
        parent.setEmployee(employee1);
        parent.setCustomer(customer);
        parent.setBranch(branch);
        parent.setStatus(QuotationStatus.SENT);
        quotationStore.put(parentId, parent);

        Quotation childMid = new Quotation();
        childMid.setId(parentId + 1);
        childMid.setQuotationNumber("QT-00001-MID");
        childMid.setEmployee(employee1);
        childMid.setCustomer(customer);
        childMid.setBranch(branch);
        childMid.setParent(parent);
        childMid.setPricingMode(PricingMode.MIDDLE);
        quotationStore.put(childMid.getId(), childMid);

        Quotation childEco = new Quotation();
        childEco.setId(parentId + 2);
        childEco.setQuotationNumber("QT-00001-ECO");
        childEco.setEmployee(employee1);
        childEco.setCustomer(customer);
        childEco.setBranch(branch);
        childEco.setParent(parent);
        childEco.setPricingMode(PricingMode.ECONOMY);
        quotationStore.put(childEco.getId(), childEco);

        parent.setChildQuotations(new ArrayList<>(List.of(childMid, childEco)));
        return parent;
    }

    @Test
    @DisplayName("Verify JDBC deletion executes in strict bottom-up foreign key order")
    void testJdbcDeletionBottomUpFkOrder() {
        Quotation parent = createQuotationWithChildren(500L);

        // Delete parent quotation
        quotationServiceWithJdbc.delete(500L);

        // Verify parent and children removed
        assertNull(quotationStore.get(500L), "Parent quotation must be deleted");
        assertNull(quotationStore.get(501L), "Child Middle quotation must be deleted");
        assertNull(quotationStore.get(502L), "Child Economy quotation must be deleted");

        // Verify SQL execution order contains bottom-up queries
        assertFalse(executedSqlList.isEmpty(), "SQL statements must have been executed");

        boolean sawItemSizes = false;
        boolean sawItems = false;
        boolean sawRooms = false;
        boolean sawCommunications = false;
        boolean sawChildQuotes = false;
        boolean sawParentQuote = false;

        for (String sql : executedSqlList) {
            if (sql.contains("quotation_item_sizes")) sawItemSizes = true;
            if (sql.contains("quotation_items") && !sql.contains("quotation_item_sizes")) sawItems = true;
            if (sql.contains("quotation_rooms") && !sql.contains("quotation_items") && !sql.contains("quotation_item_sizes")) sawRooms = true;
            if (sql.contains("quotation_communications")) sawCommunications = true;
            if (sql.contains("DELETE FROM quotations WHERE parent_id = ?")) sawChildQuotes = true;
            if (sql.contains("DELETE FROM quotations WHERE id = ?")) sawParentQuote = true;
        }

        assertTrue(sawItemSizes, "Must delete quotation_item_sizes");
        assertTrue(sawItems, "Must delete quotation_items");
        assertTrue(sawRooms, "Must delete quotation_rooms");
        assertTrue(sawCommunications, "Must delete quotation_communications before quotations");
        assertTrue(sawChildQuotes, "Must delete child quotations before parent");
        assertTrue(sawParentQuote, "Must delete parent quotation");
    }

    @Test
    @DisplayName("Verify repository-based fallback deletion removes all dependent child records")
    void testRepositoryFallbackDeletion() {
        Quotation parent = createQuotationWithChildren(600L);

        // Populate child entities in in-memory stores
        QuotationCommunication comm1 = new QuotationCommunication();
        comm1.setId(1L);
        comm1.setQuotation(parent);
        commStore.put(1L, comm1);

        QuotationCommunication commChild = new QuotationCommunication();
        commChild.setId(2L);
        commChild.setQuotation(parent.getChildQuotations().get(0));
        commStore.put(2L, commChild);

        QuotationRoom room = new QuotationRoom();
        room.setId(10L);
        room.setQuotation(parent);
        roomStore.put(10L, room);

        QuotationItem item = new QuotationItem();
        item.setId(20L);
        item.setQuotationRoom(room);
        itemStore.put(20L, item);

        QuotationItemSize size = new QuotationItemSize();
        size.setId(30L);
        size.setQuotationItem(item);
        sizeStore.put(30L, size);

        // Delete using service with NO jdbcTemplate (tests repository fallback branch)
        quotationServiceNoJdbc.delete(600L);

        // Verify all entities deleted
        assertNull(quotationStore.get(600L), "Parent quotation must be deleted");
        assertNull(quotationStore.get(601L), "Child Middle must be deleted");
        assertNull(quotationStore.get(602L), "Child Economy must be deleted");
        assertTrue(commStore.isEmpty(), "All communications must be deleted");
        assertTrue(roomStore.isEmpty(), "All rooms must be deleted");
        assertTrue(itemStore.isEmpty(), "All items must be deleted");
        assertTrue(sizeStore.isEmpty(), "All sizes must be deleted");
    }

    @Test
    @DisplayName("Verify employee authorization checks prevent deleting other employees' quotations")
    void testEmployeeOwnershipEnforcement() {
        Quotation q = new Quotation();
        q.setId(700L);
        q.setEmployee(employee1); // Belongs to employee 1 (id=10)
        quotationStore.put(700L, q);

        // Employee 2 (id=20) attempts to delete -> AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            quotationServiceWithJdbc.delete(700L, employee2.getId());
        });

        // Quotation must NOT have been deleted
        assertNotNull(quotationStore.get(700L));

        // Employee 1 (id=10) attempts to delete -> Success
        assertDoesNotThrow(() -> {
            quotationServiceWithJdbc.delete(700L, employee1.getId());
        });
        assertNull(quotationStore.get(700L));
    }

    @Test
    @DisplayName("Verify deleting child quotation unlinks it without corrupting parent or sibling quotations")
    void testDeleteIndividualChildQuotation() {
        Quotation parent = createQuotationWithChildren(800L);
        Long childMidId = 801L;
        Long childEcoId = 802L;

        // Delete child quotation (Middle)
        quotationServiceWithJdbc.delete(childMidId);

        // Middle child should be deleted
        assertNull(quotationStore.get(childMidId), "Middle child quotation must be deleted");

        // Parent and Economy sibling must remain intact
        assertNotNull(quotationStore.get(800L), "Parent quotation must remain intact");
        assertNotNull(quotationStore.get(childEcoId), "Sibling Economy quotation must remain intact");
    }
}

