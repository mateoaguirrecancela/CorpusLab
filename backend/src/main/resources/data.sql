-- CorpusLab test dataset
-- Purpose: seed enough complete and varied data for manual and integration-style testing.
-- Note: all users share the same password hash provided by the request.

BEGIN;

TRUNCATE TABLE
    notifications,
    password_reset_tokens,
    dataset_items,
    guidelines,
    labels,
    project_participants,
    projects,
    research_group_invitations,
    research_group_members,
    research_groups,
    users
RESTART IDENTITY CASCADE;

-- -----------------------------------------------------------------------------
-- Users
-- -----------------------------------------------------------------------------
INSERT INTO users (
    id,
    email,
    first_name,
    last_name,
    birth,
    gender,
    country_code,
    city,
    password_hash,
    created_at,
    updated_at
) VALUES
    (1,  'ana.owner@corpuslab.test',      'Ana',    'Lago',     DATE '1988-03-12', 'FEMALE', 'ES', 'A Coruna',  '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '140 days', now() - interval '3 days'),
    (2,  'carlos.admin@corpuslab.test',   'Carlos', 'Souto',    DATE '1986-09-21', 'MALE',   'ES', 'Vigo',      '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '138 days', now() - interval '2 days'),
    (3,  'laura.annotator@corpuslab.test','Laura',  'Pena',     DATE '1994-05-02', 'FEMALE', 'ES', 'Santiago',  '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '130 days', now() - interval '1 day'),
    (4,  'diego.annotator@corpuslab.test','Diego',  'Blanco',   DATE '1991-11-14', 'MALE',   'ES', 'Ourense',   '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '128 days', now() - interval '9 hours'),
    (5,  'marta.owner@corpuslab.test',    'Marta',  'Costa',    DATE '1989-07-03', 'FEMALE', 'PT', 'Porto',     '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '120 days', now() - interval '5 days'),
    (6,  'pedro.admin@corpuslab.test',    'Pedro',  'Silva',    DATE '1987-01-25', 'MALE',   'PT', 'Lisbon',    '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '116 days', now() - interval '2 days'),
    (7,  'irene.annotator@corpuslab.test','Irene',  'Ramos',    DATE '1996-08-30', 'FEMALE', 'ES', 'Madrid',    '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '112 days', now() - interval '3 hours'),
    (8,  'pablo.annotator@corpuslab.test','Pablo',  'Gil',      DATE '1993-12-19', 'MALE',   'ES', 'Bilbao',    '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '108 days', now() - interval '6 hours'),
    (9,  'noelia.owner@corpuslab.test',   'Noelia', 'Rey',      DATE '1985-02-11', 'FEMALE', 'ES', 'Valencia',  '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '102 days', now() - interval '4 days'),
    (10, 'hugo.annotator@corpuslab.test', 'Hugo',   'Martin',   DATE '1997-04-09', 'MALE',   'ES', 'Seville',   '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '98 days',  now() - interval '36 hours'),
    (11, 'sofia.invited@corpuslab.test',  'Sofia',  'Alonso',   DATE '1998-06-17', 'FEMALE', 'ES', 'Zaragoza',  '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '90 days',  now() - interval '11 hours'),
    (12, 'lucia.legal@corpuslab.test',    'Lucia',  'Pardo',    DATE '1992-10-07', 'OTHER',  'ES', 'Barcelona', '$2a$10$K8zABl.zfx4mXMGQYb7QgO/S6gzWv2RYCyCPfgk4JRmvS.czW9hfi', now() - interval '84 days',  now() - interval '1 hour');

-- -----------------------------------------------------------------------------
-- Research groups, memberships, invitations
-- -----------------------------------------------------------------------------
INSERT INTO research_groups (id, name, description, invitation_code, created_at) VALUES
    (1, 'Language Analytics Hub', 'Group focused on multilingual text analytics.', 'LANGUAGE-HUB-TEST-AAA111', now() - interval '120 days'),
    (2, 'Biomedical NLP Lab', 'Clinical and biomedical annotation workflows.', 'BIOMED-NLP-TEST-BBB222', now() - interval '118 days'),
    (3, 'Legal AI Forge', 'Legal-domain datasets and extraction pipelines.', 'LEGAL-AI-TEST-CCC333', now() - interval '110 days');

INSERT INTO research_group_members (id, role, joined_at, deleted_at, user_id, research_group_id) VALUES
    (1,  'OWNER',     now() - interval '119 days', NULL,                    1,  1),
    (2,  'ADMIN',     now() - interval '118 days', NULL,                    2,  1),
    (3,  'ANNOTATOR', now() - interval '116 days', NULL,                    3,  1),
    (4,  'ANNOTATOR', now() - interval '115 days', NULL,                    4,  1),
    (5,  'ANNOTATOR', now() - interval '100 days', now() - interval '20 days', 10, 1),
    (6,  'OWNER',     now() - interval '117 days', NULL,                    5,  2),
    (7,  'ADMIN',     now() - interval '116 days', NULL,                    6,  2),
    (8,  'ANNOTATOR', now() - interval '112 days', NULL,                    7,  2),
    (9,  'ANNOTATOR', now() - interval '111 days', NULL,                    8,  2),
    (10, 'ANNOTATOR', now() - interval '95 days',  NULL,                    11, 2),
    (11, 'OWNER',     now() - interval '109 days', NULL,                    9,  3),
    (12, 'ADMIN',     now() - interval '106 days', NULL,                    12, 3),
    (13, 'ANNOTATOR', now() - interval '92 days',  NULL,                    4,  3),
    (14, 'ANNOTATOR', now() - interval '91 days',  NULL,                    6,  3);

INSERT INTO research_group_invitations (
    id,
    research_group_id,
    inviter_user_id,
    invited_user_id,
    invited_email,
    token,
    role,
    status,
    created_at,
    expires_at
) VALUES
    (1, 1, 1,  NULL, 'new.researcher@corp.test', 'rg1-token-pending-001',  'ANNOTATOR', 'PENDING',  now() - interval '6 days',  now() + interval '8 days'),
    (2, 2, 5,  11,   'sofia.invited@corpuslab.test', 'rg2-token-accepted-002', 'ANNOTATOR', 'ACCEPTED', now() - interval '28 days', now() + interval '2 days'),
    (3, 3, 9,  10,   'hugo.annotator@corpuslab.test', 'rg3-token-declined-003', 'ADMIN',     'DECLINED', now() - interval '32 days', now() - interval '3 days'),
    (4, 1, 2,  NULL, 'linguist.external@corp.test', 'rg1-token-pending-004',  'ANNOTATOR', 'PENDING',  now() - interval '2 days',  now() + interval '12 days'),
    (5, 2, 6,  NULL, 'clinician.external@corp.test', 'rg2-token-pending-005',  'ANNOTATOR', 'PENDING',  now() - interval '1 day',   now() + interval '14 days'),
    (6, 3, 12, NULL, 'legal.intern@corp.test',      'rg3-token-pending-006',  'ANNOTATOR', 'PENDING',  now() - interval '7 hours', now() + interval '10 days');

-- -----------------------------------------------------------------------------
-- Projects and participants
-- -----------------------------------------------------------------------------
INSERT INTO projects (
    id,
    research_group_id,
    name,
    description,
    project_type,
    setup_completed,
    is_archived,
    created_at
) VALUES
    (1, 1, 'News Sentiment Baseline', 'Binary and neutral sentiment over short news snippets.', 'TEXT_CLASSIFICATION_SIMPLE',     true,  false, now() - interval '60 days'),
    (2, 1, 'Multi Topic News',        'Multi-label topical annotation for newsroom content.',    'TEXT_CLASSIFICATION_MULTILABEL', true,  false, now() - interval '50 days'),
    (3, 2, 'Clinical NER Corpus',     'Named entity extraction in clinical text and JSON.',       'NER',                           true,  false, now() - interval '45 days'),
    (4, 2, 'Meeting Note Summaries',  'Abstractive summaries of operational notes.',              'SEQ2SEQ',                       true,  false, now() - interval '40 days'),
    (5, 3, 'Clause Classification Pilot', 'Early pilot with partial setup to test flow guards.',  'TEXT_CLASSIFICATION_SIMPLE',    false, false, now() - interval '20 days'),
    (6, 3, 'Legal NER Extraction',    'Extract judges, laws, organizations and dates.',           'NER',                           true,  false, now() - interval '35 days'),
    (7, 1, 'EN-ES Sentence Rewrites', 'Controlled rewriting and translation-like tasks.',         'SEQ2SEQ',                       true,  false, now() - interval '30 days'),
    (8, 2, 'Trial Report Tagging',    'Tagging trial reports with multiple dimensions.',          'TEXT_CLASSIFICATION_MULTILABEL', true, false, now() - interval '25 days');

INSERT INTO project_participants (id, project_id, user_id, role, assigned_at) VALUES
    (1,  1, 1,  'CREATOR',    now() - interval '60 days'),
    (2,  1, 2,  'PARTICIPANT', now() - interval '59 days'),
    (3,  1, 3,  'PARTICIPANT', now() - interval '58 days'),
    (4,  1, 4,  'PARTICIPANT', now() - interval '58 days'),

    (5,  2, 2,  'CREATOR',    now() - interval '50 days'),
    (6,  2, 1,  'PARTICIPANT', now() - interval '49 days'),
    (7,  2, 3,  'PARTICIPANT', now() - interval '49 days'),
    (8,  2, 4,  'PARTICIPANT', now() - interval '48 days'),
    (9,  2, 10, 'PARTICIPANT', now() - interval '48 days'),

    (10, 3, 5,  'CREATOR',    now() - interval '45 days'),
    (11, 3, 6,  'PARTICIPANT', now() - interval '44 days'),
    (12, 3, 7,  'PARTICIPANT', now() - interval '44 days'),
    (13, 3, 8,  'PARTICIPANT', now() - interval '44 days'),

    (14, 4, 5,  'CREATOR',    now() - interval '40 days'),
    (15, 4, 8,  'PARTICIPANT', now() - interval '39 days'),
    (16, 4, 11, 'PARTICIPANT', now() - interval '39 days'),

    (17, 5, 9,  'CREATOR',    now() - interval '20 days'),
    (18, 5, 12, 'PARTICIPANT', now() - interval '19 days'),
    (19, 5, 4,  'PARTICIPANT', now() - interval '19 days'),

    (20, 6, 9,  'CREATOR',    now() - interval '35 days'),
    (21, 6, 12, 'PARTICIPANT', now() - interval '34 days'),
    (22, 6, 6,  'PARTICIPANT', now() - interval '34 days'),

    (23, 7, 1,  'CREATOR',    now() - interval '30 days'),
    (24, 7, 3,  'PARTICIPANT', now() - interval '29 days'),
    (25, 7, 10, 'PARTICIPANT', now() - interval '29 days'),

    (26, 8, 6,  'CREATOR',    now() - interval '25 days'),
    (27, 8, 7,  'PARTICIPANT', now() - interval '24 days'),
    (28, 8, 8,  'PARTICIPANT', now() - interval '24 days'),
    (29, 8, 11, 'PARTICIPANT', now() - interval '24 days');

-- -----------------------------------------------------------------------------
-- Labels and guidelines
-- -----------------------------------------------------------------------------
INSERT INTO labels (id, project_id, name, color, created_at) VALUES
    (1, 1, 'POSITIVE', '#22C55E', now() - interval '60 days'),
    (2, 1, 'NEGATIVE', '#EF4444', now() - interval '60 days'),
    (3, 1, 'NEUTRAL',  '#64748B', now() - interval '60 days'),

    (4, 2, 'POLITICA', '#F59E0B', now() - interval '50 days'),
    (5, 2, 'ECONOMIA', '#10B981', now() - interval '50 days'),
    (6, 2, 'DEPORTES', '#3B82F6', now() - interval '50 days'),
    (7, 2, 'CIENCIA',  '#8B5CF6', now() - interval '50 days'),
    (8, 2, 'CULTURA',  '#EC4899', now() - interval '50 days'),

    (9,  3, 'PERSON',    '#FB7185', now() - interval '45 days'),
    (10, 3, 'ORG',       '#60A5FA', now() - interval '45 days'),
    (11, 3, 'LOCATION',  '#34D399', now() - interval '45 days'),
    (12, 3, 'BIOMARKER', '#A78BFA', now() - interval '45 days'),

    (13, 6, 'JUDGE',    '#F97316', now() - interval '35 days'),
    (14, 6, 'LAW',      '#0EA5E9', now() - interval '35 days'),
    (15, 6, 'ORG',      '#22C55E', now() - interval '35 days'),
    (16, 6, 'LOCATION', '#14B8A6', now() - interval '35 days'),
    (17, 6, 'DATE',     '#EAB308', now() - interval '35 days'),

    (18, 8, 'SAFETY',     '#EF4444', now() - interval '25 days'),
    (19, 8, 'EFFICACY',   '#22C55E', now() - interval '25 days'),
    (20, 8, 'TRIALS',     '#3B82F6', now() - interval '25 days'),
    (21, 8, 'REGULATION', '#A855F7', now() - interval '25 days');

INSERT INTO guidelines (id, project_id, content, file_url, created_at, updated_at) VALUES
    (1, 1, 'Label sentiment by final tone. Use NEUTRAL when evidence is mixed or purely informational.', NULL, now() - interval '59 days', now() - interval '5 days'),
    (2, 2, 'Assign all applicable topics. Prefer precision over coverage when uncertain.', NULL, now() - interval '49 days', now() - interval '6 days'),
    (3, 3, 'Mark only explicit entities. Do not infer hidden mentions.', NULL, now() - interval '44 days', now() - interval '2 days'),
    (4, 4, NULL,
        'data:application/pdf;base64,' || encode(convert_to(E'%PDF-1.1\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<<>>\n%%EOF', 'UTF8'), 'base64'),
        now() - interval '39 days', now() - interval '39 days'),
    (5, 6, 'Legal NER: include judge names, law references, organizations and explicit dates only.', NULL, now() - interval '34 days', now() - interval '2 days'),
    (6, 7, 'Rewrite preserving meaning and factual constraints. Keep output concise.', NULL, now() - interval '29 days', now() - interval '1 day'),
    (7, 8, 'Use as many labels as needed when report spans safety, efficacy and compliance.', NULL, now() - interval '24 days', now() - interval '8 hours');

-- -----------------------------------------------------------------------------
-- Dataset items (JSONB content with base64 + optional annotationsByUser)
-- -----------------------------------------------------------------------------
INSERT INTO dataset_items (id, project_id, item_index, content, created_at) VALUES
    (
        1,
        1,
        0,
        jsonb_build_object(
            'fileName', 'sentiment_batch_a.csv',
            'mimeType', 'text/csv',
            'sizeBytes', octet_length(convert_to(E'id,text\n1,Team won the final\n2,Unemployment rose this month\n3,City opens a new library', 'UTF8')),
            'base64', encode(convert_to(E'id,text\n1,Team won the final\n2,Unemployment rose this month\n3,City opens a new library', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '3', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('label', 'POSITIVE', 'notes', 'Sports event'),
                    '1', jsonb_build_object('label', 'NEGATIVE')
                )),
                '4', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('label', 'POSITIVE'),
                    '1', jsonb_build_object('label', 'NEGATIVE'),
                    '2', jsonb_build_object('label', 'NEUTRAL', 'notes', 'Institutional info')
                ))
            )
        ),
        now() - interval '58 days'
    ),
    (
        2,
        1,
        1,
        jsonb_build_object(
            'fileName', 'sentiment_batch_b.csv',
            'mimeType', 'text/csv',
            'sizeBytes', octet_length(convert_to(E'id,text\n4,Stocks closed in green\n5,Storm damaged the coastal road', 'UTF8')),
            'base64', encode(convert_to(E'id,text\n4,Stocks closed in green\n5,Storm damaged the coastal road', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '2', jsonb_build_object('steps', jsonb_build_object(
                    '1', jsonb_build_object('label', 'NEGATIVE')
                )),
                '3', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('label', 'POSITIVE')
                ))
            )
        ),
        now() - interval '57 days'
    ),
    (
        3,
        2,
        0,
        jsonb_build_object(
            'fileName', 'topic_news_001.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('The government announced a joint plan on energy and jobs.', 'UTF8')),
            'base64', encode(convert_to('The government announced a joint plan on energy and jobs.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '3', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('labels', jsonb_build_array('POLITICA', 'ECONOMIA'))
                )),
                '4', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('labels', jsonb_build_array('ECONOMIA', 'CIENCIA'))
                ))
            )
        ),
        now() - interval '49 days'
    ),
    (
        4,
        2,
        1,
        jsonb_build_object(
            'fileName', 'topic_news_002.json',
            'mimeType', 'application/json',
            'sizeBytes', octet_length(convert_to('{"id":1001,"title":"Education reform and funding","keywords":["politics","economy","culture"]}', 'UTF8')),
            'base64', encode(convert_to('{"id":1001,"title":"Education reform and funding","keywords":["politics","economy","culture"]}', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '3', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('labels', jsonb_build_array('POLITICA', 'ECONOMIA', 'CULTURA'))
                ))
            )
        ),
        now() - interval '49 days'
    ),
    (
        5,
        2,
        2,
        jsonb_build_object(
            'fileName', 'topic_news_003.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('The city derby generated tourism and cultural activity.', 'UTF8')),
            'base64', encode(convert_to('The city derby generated tourism and cultural activity.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '10', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('labels', jsonb_build_array('DEPORTES', 'ECONOMIA', 'CULTURA'))
                ))
            )
        ),
        now() - interval '48 days'
    ),
    (
        6,
        3,
        0,
        jsonb_build_object(
            'fileName', 'clinical_ner_001.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('John works at OpenAI in San Francisco.', 'UTF8')),
            'base64', encode(convert_to('John works at OpenAI in San Francisco.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '7', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object(
                        'entities', jsonb_build_array(
                            jsonb_build_object('label', 'PERSON',   'text', 'John',          'startOffset', 0,  'endOffset', 4),
                            jsonb_build_object('label', 'ORG',      'text', 'OpenAI',        'startOffset', 14, 'endOffset', 20),
                            jsonb_build_object('label', 'LOCATION', 'text', 'San Francisco', 'startOffset', 24, 'endOffset', 37)
                        ),
                        'notes', 'Main entities detected'
                    )
                )),
                '8', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object(
                        'entities', jsonb_build_array(
                            jsonb_build_object('label', 'ORG', 'text', 'OpenAI', 'startOffset', 14, 'endOffset', 20)
                        )
                    )
                ))
            )
        ),
        now() - interval '44 days'
    ),
    (
        7,
        3,
        1,
        jsonb_build_object(
            'fileName', 'clinical_ner_002.json',
            'mimeType', 'application/json',
            'sizeBytes', octet_length(convert_to('{"patient":"Maria Gomez","hospital":"CHUAC","city":"A Coruna"}', 'UTF8')),
            'base64', encode(convert_to('{"patient":"Maria Gomez","hospital":"CHUAC","city":"A Coruna"}', 'UTF8'), 'base64')
        ),
        now() - interval '44 days'
    ),
    (
        8,
        3,
        2,
        jsonb_build_object(
            'fileName', 'clinical_ner_003.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('Biomarker TP53 increased in sample S-100.', 'UTF8')),
            'base64', encode(convert_to('Biomarker TP53 increased in sample S-100.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '7', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object(
                        'entities', jsonb_build_array(
                            jsonb_build_object('label', 'BIOMARKER', 'text', 'TP53', 'startOffset', 10, 'endOffset', 14)
                        )
                    )
                ))
            )
        ),
        now() - interval '43 days'
    ),
    (
        9,
        4,
        0,
        jsonb_build_object(
            'fileName', 'meeting_note_001.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('Team discussed sprint blockers, timeline risk, and mitigation plan.', 'UTF8')),
            'base64', encode(convert_to('Team discussed sprint blockers, timeline risk, and mitigation plan.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '8', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('text', 'The team reviewed blockers, identified schedule risk, and defined mitigations.')
                ))
            )
        ),
        now() - interval '39 days'
    ),
    (
        10,
        4,
        1,
        jsonb_build_object(
            'fileName', 'meeting_attachment.pdf',
            'mimeType', 'application/pdf',
            'sizeBytes', octet_length(convert_to(E'%PDF-1.1\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF', 'UTF8')),
            'base64', encode(convert_to(E'%PDF-1.1\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF', 'UTF8'), 'base64')
        ),
        now() - interval '39 days'
    ),
    (
        11,
        4,
        2,
        jsonb_build_object(
            'fileName', 'meeting_slide.svg',
            'mimeType', 'image/svg+xml',
            'sizeBytes', octet_length(convert_to('<svg xmlns="http://www.w3.org/2000/svg" width="240" height="80"><rect width="240" height="80" fill="#1D4ED8"/><text x="12" y="48" fill="white" font-size="20">CorpusLab</text></svg>', 'UTF8')),
            'base64', encode(convert_to('<svg xmlns="http://www.w3.org/2000/svg" width="240" height="80"><rect width="240" height="80" fill="#1D4ED8"/><text x="12" y="48" fill="white" font-size="20">CorpusLab</text></svg>', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '11', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('text', 'Slide shows project identity and branding only.')
                ))
            )
        ),
        now() - interval '38 days'
    ),
    (
        12,
        5,
        0,
        jsonb_build_object(
            'fileName', 'legal_pilot.csv',
            'mimeType', 'text/csv',
            'sizeBytes', octet_length(convert_to(E'id,text\n1,Clause allows unilateral termination\n2,Clause defines penalty on delay', 'UTF8')),
            'base64', encode(convert_to(E'id,text\n1,Clause allows unilateral termination\n2,Clause defines penalty on delay', 'UTF8'), 'base64')
        ),
        now() - interval '19 days'
    ),
    (
        13,
        6,
        0,
        jsonb_build_object(
            'fileName', 'legal_ner_001.json',
            'mimeType', 'application/json',
            'sizeBytes', octet_length(convert_to('{"judge":"Elena Ruiz","law":"12/2020","city":"Madrid","date":"2026-03-01"}', 'UTF8')),
            'base64', encode(convert_to('{"judge":"Elena Ruiz","law":"12/2020","city":"Madrid","date":"2026-03-01"}', 'UTF8'), 'base64')
        ),
        now() - interval '34 days'
    ),
    (
        14,
        6,
        1,
        jsonb_build_object(
            'fileName', 'legal_ner_002.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('Judge Lopez cited Law 12/2020 in Madrid on 2026-03-01.', 'UTF8')),
            'base64', encode(convert_to('Judge Lopez cited Law 12/2020 in Madrid on 2026-03-01.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '6', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object(
                        'entities', jsonb_build_array(
                            jsonb_build_object('label', 'JUDGE',    'text', 'Lopez',      'startOffset', 6,  'endOffset', 11),
                            jsonb_build_object('label', 'LAW',      'text', 'Law 12/2020','startOffset', 18, 'endOffset', 29),
                            jsonb_build_object('label', 'LOCATION', 'text', 'Madrid',     'startOffset', 33, 'endOffset', 39),
                            jsonb_build_object('label', 'DATE',     'text', '2026-03-01', 'startOffset', 43, 'endOffset', 53)
                        )
                    )
                )),
                '12', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object(
                        'entities', jsonb_build_array(
                            jsonb_build_object('label', 'JUDGE', 'text', 'Lopez', 'startOffset', 6, 'endOffset', 11),
                            jsonb_build_object('label', 'LAW',   'text', 'Law 12/2020', 'startOffset', 18, 'endOffset', 29)
                        )
                    )
                ))
            )
        ),
        now() - interval '33 days'
    ),
    (
        15,
        7,
        0,
        jsonb_build_object(
            'fileName', 'rewrite_001.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('Original: The committee approved the revised protocol yesterday.', 'UTF8')),
            'base64', encode(convert_to('Original: The committee approved the revised protocol yesterday.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '3', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('text', 'Rewritten: Yesterday, the committee approved the updated protocol.')
                ))
            )
        ),
        now() - interval '29 days'
    ),
    (
        16,
        7,
        1,
        jsonb_build_object(
            'fileName', 'rewrite_002.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('Original: Analysts requested stronger evidence before deployment.', 'UTF8')),
            'base64', encode(convert_to('Original: Analysts requested stronger evidence before deployment.', 'UTF8'), 'base64')
        ),
        now() - interval '28 days'
    ),
    (
        17,
        8,
        0,
        jsonb_build_object(
            'fileName', 'trial_reports.csv',
            'mimeType', 'text/csv',
            'sizeBytes', octet_length(convert_to(E'id,text\n1,Phase II trial reports mild adverse events\n2,Primary endpoint met in cohort B\n3,Agency requested additional monitoring', 'UTF8')),
            'base64', encode(convert_to(E'id,text\n1,Phase II trial reports mild adverse events\n2,Primary endpoint met in cohort B\n3,Agency requested additional monitoring', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '7', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('labels', jsonb_build_array('SAFETY', 'TRIALS')),
                    '1', jsonb_build_object('labels', jsonb_build_array('EFFICACY', 'TRIALS'))
                )),
                '8', jsonb_build_object('steps', jsonb_build_object(
                    '2', jsonb_build_object('labels', jsonb_build_array('REGULATION', 'SAFETY'))
                ))
            )
        ),
        now() - interval '24 days'
    ),
    (
        18,
        8,
        1,
        jsonb_build_object(
            'fileName', 'trial_reports_extra.txt',
            'mimeType', 'text/plain',
            'sizeBytes', octet_length(convert_to('Regulator approved expanded access after strong efficacy data.', 'UTF8')),
            'base64', encode(convert_to('Regulator approved expanded access after strong efficacy data.', 'UTF8'), 'base64'),
            'annotationsByUser', jsonb_build_object(
                '11', jsonb_build_object('steps', jsonb_build_object(
                    '0', jsonb_build_object('labels', jsonb_build_array('REGULATION', 'EFFICACY'))
                ))
            )
        ),
        now() - interval '24 days'
    );

-- -----------------------------------------------------------------------------
-- Password reset tokens
-- -----------------------------------------------------------------------------
INSERT INTO password_reset_tokens (id, token, user_id, expiry_date) VALUES
    (1, 'reset-token-user4-active',  4,  now() + interval '2 days'),
    (2, 'reset-token-user7-expired', 7,  now() - interval '1 day'),
    (3, 'reset-token-user12-active', 12, now() + interval '5 days');

-- -----------------------------------------------------------------------------
-- Notifications
-- -----------------------------------------------------------------------------
INSERT INTO notifications (
    id,
    recipient_user_id,
    actor_user_id,
    type,
    research_group_id,
    research_group_name,
    invitation_id,
    project_id,
    project_name,
    read_at,
    created_at
) VALUES
    (1,  11, 5,  'RESEARCH_GROUP_INVITATION_RECEIVED', 2, 'Biomedical NLP Lab', 2, NULL, NULL, NULL,                    now() - interval '12 days'),
    (2,  5,  11, 'RESEARCH_GROUP_INVITATION_ACCEPTED', 2, 'Biomedical NLP Lab', 2, NULL, NULL, now() - interval '10 days', now() - interval '11 days'),
    (3,  4,  1,  'PROJECT_PARTICIPANT_ASSIGNED',       1, 'Language Analytics Hub', NULL, 1, 'News Sentiment Baseline', NULL,                    now() - interval '9 days'),
    (4,  3,  2,  'PROJECT_PARTICIPANT_ASSIGNED',       1, 'Language Analytics Hub', NULL, 2, 'Multi Topic News', now() - interval '7 days', now() - interval '8 days'),
    (5,  1,  3,  'PROJECT_ANNOTATION_COMPLETED',       1, 'Language Analytics Hub', NULL, 1, 'News Sentiment Baseline', NULL,                    now() - interval '6 days'),
    (6,  5,  7,  'PROJECT_ANNOTATION_COMPLETED',       2, 'Biomedical NLP Lab',    NULL, 3, 'Clinical NER Corpus', now() - interval '4 days', now() - interval '5 days'),
    (7,  12, 9,  'PROJECT_PARTICIPANT_ASSIGNED',       3, 'Legal AI Forge',         NULL, 6, 'Legal NER Extraction', NULL,                    now() - interval '4 days'),
    (8,  9,  12, 'PROJECT_ANNOTATION_COMPLETED',       3, 'Legal AI Forge',         NULL, 6, 'Legal NER Extraction', NULL,                    now() - interval '3 days'),
    (9,  10, 9,  'RESEARCH_GROUP_INVITATION_RECEIVED', 3, 'Legal AI Forge',         3, NULL, NULL, now() - interval '13 days', now() - interval '14 days'),
    (10, 6,  5,  'PROJECT_PARTICIPANT_ASSIGNED',       2, 'Biomedical NLP Lab',    NULL, 3, 'Clinical NER Corpus', NULL,                    now() - interval '2 days'),
    (11, 11, 6,  'PROJECT_PARTICIPANT_ASSIGNED',       2, 'Biomedical NLP Lab',    NULL, 8, 'Trial Report Tagging', NULL,                    now() - interval '36 hours'),
    (12, 1,  4,  'PROJECT_ANNOTATION_COMPLETED',       1, 'Language Analytics Hub', NULL, 7, 'EN-ES Sentence Rewrites', now() - interval '10 hours', now() - interval '12 hours');

-- -----------------------------------------------------------------------------
-- Keep identity sequences aligned with explicit IDs
-- -----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('research_groups', 'id'), COALESCE((SELECT MAX(id) FROM research_groups), 1), true);
SELECT setval(pg_get_serial_sequence('research_group_members', 'id'), COALESCE((SELECT MAX(id) FROM research_group_members), 1), true);
SELECT setval(pg_get_serial_sequence('research_group_invitations', 'id'), COALESCE((SELECT MAX(id) FROM research_group_invitations), 1), true);
SELECT setval(pg_get_serial_sequence('projects', 'id'), COALESCE((SELECT MAX(id) FROM projects), 1), true);
SELECT setval(pg_get_serial_sequence('project_participants', 'id'), COALESCE((SELECT MAX(id) FROM project_participants), 1), true);
SELECT setval(pg_get_serial_sequence('labels', 'id'), COALESCE((SELECT MAX(id) FROM labels), 1), true);
SELECT setval(pg_get_serial_sequence('guidelines', 'id'), COALESCE((SELECT MAX(id) FROM guidelines), 1), true);
SELECT setval(pg_get_serial_sequence('dataset_items', 'id'), COALESCE((SELECT MAX(id) FROM dataset_items), 1), true);
SELECT setval(pg_get_serial_sequence('password_reset_tokens', 'id'), COALESCE((SELECT MAX(id) FROM password_reset_tokens), 1), true);
SELECT setval(pg_get_serial_sequence('notifications', 'id'), COALESCE((SELECT MAX(id) FROM notifications), 1), true);

COMMIT;
