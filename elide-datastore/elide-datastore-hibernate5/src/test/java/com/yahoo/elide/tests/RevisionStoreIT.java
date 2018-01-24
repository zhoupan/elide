package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
import example.Person;
import example.TestCheckMappings;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class RevisionStoreIT extends AbstractIntegrationTestInitializer {

    private final JsonParser jsonParser;
    private final ObjectMapper mapper;
    private final Elide elide;
    private static EntityDictionary entityDictionary;

    private static final String CLASH_OF_KINGS = "A Clash of Kings";
    private static final String STORM_OF_SWORDS = "A Storm of Swords";
    private static final String SONG_OF_ICE_AND_FIRE = "A Song of Ice and Fire";
    private static final String DATA = "data";
    private static final String ATTRIBUTES = "attributes";
    private static final String TITLE = "title";
    private static final String CHAPTER_COUNT = "chapterCount";

    public RevisionStoreIT() {
        jsonParser = new JsonParser();
        mapper = new ObjectMapper();
        entityDictionary = new EntityDictionary(TestCheckMappings.MAPPINGS);
        elide = new Elide(new ElideSettingsBuilder(AbstractIntegrationTestInitializer.getDatabaseManager())
                .withAuditLogger(new TestAuditLogger())
                .withEntityDictionary(entityDictionary)
                .build());
    }

    @BeforeClass
    public static void setup() throws IOException {
        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            Person georgeMartin = new Person();
            georgeMartin.setId(1L);
            georgeMartin.setName("George R. R. Martin");
            tx.save(georgeMartin, null);
            tx.commit(null);
        }

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            RequestScope rs = Mockito.mock(RequestScope.class);
            Mockito.when(rs.getDictionary()).thenReturn(entityDictionary);
            Mockito.when(rs.isHistorical()).thenReturn(false);
            Person revPerson = (Person) tx.loadObject(Person.class, 1L, Optional.empty(), rs);
            revPerson.setName("Numerology");
            tx.save(revPerson, null);
            tx.commit(null);
        }

    }

    @Test
    public void testSubcollectionEntityFormulaFetch() throws Exception {
        MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
        ElideResponse response = elide.get("/person", queryParams, 1);

        JsonNode result = mapper.readTree(response.getBody());
        assertEquals(result.get(DATA).size(), 3);
        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(TITLE).asText(), SONG_OF_ICE_AND_FIRE);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(TITLE).asText(), CLASH_OF_KINGS);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(TITLE).asText(), STORM_OF_SWORDS);

        assertEquals(result.get(DATA).get(0).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), 10);
        assertEquals(result.get(DATA).get(1).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), 20);
        assertEquals(result.get(DATA).get(2).get(ATTRIBUTES).get(CHAPTER_COUNT).asInt(), 30);
    }

}
