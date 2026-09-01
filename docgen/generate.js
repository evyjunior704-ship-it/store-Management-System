/**
 * StoreMS Security Playbook - DOCX Generator
 *
 * Converts the markdown chapter files into a formatted Word document with:
 * - Cover page with title and metadata
 * - Table of contents
 * - Numbered chapters with H1-H4 hierarchy
 * - Code blocks with monospace font and light gray shading
 * - Tables with proper borders
 * - Bold/italic inline formatting
 * - Learning note callouts styled as blockquotes
 *
 * Run: node generate.js
 */

const fs = require('fs');
const path = require('path');
const {
    Document,
    Packer,
    Paragraph,
    TextRun,
    HeadingLevel,
    WidthType,
    BorderStyle,
    Table,
    TableRow,
    TableCell,
    VerticalAlign,
    AlignmentType,
    ShadingType,
    PageBreak,
    SymbolRun,
    NumberingFormat,
    LevelFormat
} = require('docx');

// Configuration
const CONFIG = {
    title: 'StoreMS Security Playbook',
    subtitle: 'A Deep Dive into Spring Boot Security Patterns',
    author: 'Cybersecurity Learning Series',
    company: 'StoreMS Project',
    chaptersDir: path.join(__dirname, 'sections'),
    outputDir: path.join(__dirname, 'output'),
    // US Letter dimensions in DXA (1 inch = 1440 DXA)
    page: {
        width: 12240,  // 8.5 inches
        height: 15840, // 11 inches
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
    }
};

// Colors
const COLORS = {
    codeBg: 'EEEEEE',
    headerBg: 'F5F5F5',
    accent: '2563EB',
    text: '333333',
    muted: '666666'
};

/**
 * Parse a markdown file and extract structured content
 */
function parseMarkdown(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    const lines = content.split('\n');
    const result = {
        h1: null,
        sections: [],
        tables: [],
        codeBlocks: [],
        learningNotes: []
    };

    let currentSection = null;
    let inCodeBlock = false;
    let codeBlockContent = [];
    let codeBlockLang = '';
    let buffer = [];

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];

        // H1 (only one at the start)
        if (line.match(/^#\s+(.+)/) && !result.h1) {
            result.h1 = line.replace(/^#\s+/, '').trim();
            continue;
        }

        // Code blocks
        if (line.match(/^```/)) {
            if (!inCodeBlock) {
                inCodeBlock = true;
                codeBlockContent = [];
                codeBlockLang = line.replace(/^```/, '').trim();
            } else {
                inCodeBlock = false;
                result.codeBlocks.push({
                    content: codeBlockContent.join('\n'),
                    lang: codeBlockLang,
                    line: i - codeBlockContent.length
                });
                codeBlockContent = [];
            }
            continue;
        }

        if (inCodeBlock) {
            codeBlockContent.push(line);
            continue;
        }

        // Learning note blocks
        if (line.match(/^>\s*\*\*Learning note:\*\*/i)) {
            const note = line.replace(/^>\s*\*\*Learning note:\*\*/i, '').trim();
            result.learningNotes.push({ type: 'learning', text: note, line: i });
            continue;
        }
        if (line.match(/^>\s*\*\*Why it matters:\*\*/i)) {
            const note = line.replace(/^>\s*\*\*Why it matters:\*\*/i, '').trim();
            result.learningNotes.push({ type: 'why', text: note, line: i });
            continue;
        }

        // Section headings
        const h2Match = line.match(/^##\s+(.+)/);
        const h3Match = line.match(/^###\s+(.+)/);
        const h4Match = line.match(/^####\s+(.+)/);

        if (h2Match || h3Match || h4Match) {
            // Flush any pending buffer as paragraph text
            if (buffer.length > 0) {
                if (currentSection) {
                    currentSection.content.push({ type: 'paragraph', text: buffer.join(' ') });
                }
                buffer = [];
            }

            const level = h2Match ? 2 : h3Match ? 3 : 4;
            const title = (h2Match || h3Match || h4Match)[1];

            currentSection = {
                level,
                title,
                content: [],
                line: i
            };
            result.sections.push(currentSection);
            continue;
        }

        // Tables
        if (line.match(/^\|/) && !line.match(/^|\s*-/)) {
            const cells = line.split('|').map(c => c.trim()).filter(c => c);
            if (line.match(/^|--/)) {
                // Skip separator row
                continue;
            }
            // Check if next row is also a table or separator
            const nextLine = lines[i + 1];
            if (nextLine && nextLine.match(/^|--/)) {
                // This is a table header
                const rows = [];
                rows.push(cells);
                let j = i + 2;
                while (j < lines.length && lines[j].match(/^\|/) && !lines[j].match(/^|--/)) {
                    const rowCells = lines[j].split('|').map(c => c.trim()).filter(c => c);
                    rows.push(rowCells);
                    j++;
                }
                result.tables.push({
                    headers: cells,
                    rows: rows.slice(1),
                    line: i
                });
                i = j - 1;
                continue;
            }
        }

        // Chapter summary line
        if (line.match(/^>\s*\*\*Chapter summary:\*\*/i)) {
            if (currentSection) {
                currentSection.content.push({ type: 'summary', text: line.replace(/^>\s*\*\*Chapter summary:\*\*/i, '').trim() });
            }
            continue;
        }

        // Regular paragraph content
        if (line.trim() === '') {
            if (buffer.length > 0) {
                if (currentSection) {
                    currentSection.content.push({ type: 'paragraph', text: buffer.join(' ') });
                }
                buffer = [];
            }
        } else {
            // Clean inline formatting and add to buffer
            let text = line
                .replace(/^\s*[-*]\s*/, '')  // Remove bullet markers
                .replace(/^\s*\d+\.\s*/, '') // Remove numbered list markers
                .trim();

            if (text) {
                buffer.push(text);
            }
        }
    }

    // Flush remaining buffer
    if (buffer.length > 0 && currentSection) {
        currentSection.content.push({ type: 'paragraph', text: buffer.join(' ') });
    }

    return result;
}

/**
 * Create a table from markdown table data
 */
function createDocxTable(tableData) {
    const headerCells = tableData.headers.map(h => new TableCell({
        children: [new Paragraph({
            children: [new TextRun({ text: h, bold: true })],
            alignment: AlignmentType.CENTER
        })],
        shading: { fill: COLORS.headerBg }
    }));

    const rows = [
        // Header row
        new TableRow({ children: headerCells })
    ];

    // Data rows
    for (const row of tableData.rows) {
        const cells = row.map(cell => new TableCell({
            children: [new Paragraph({ text: cell })],
            verticalAlign: VerticalAlign.CENTER
        }));
        rows.push(new TableRow({ children: cells }));
    }

    return new Table({
        rows,
        width: { size: 100, type: WidthType.PERCENTAGE }
    });
}

/**
 * Parse inline markdown formatting (bold, italic, code)
 */
function parseInlineFormatting(text) {
    const runs = [];
    let remaining = text;
    let index = 0;

    // Code backticks
    while (remaining.length > 0) {
        const codeMatch = remaining.match(/`([^`]+)`/);
        const boldMatch = remaining.match(/\*\*([^*]+)\*\*/);
        const italicMatch = remaining.match(/\*([^*]+)\*/);

        // Find earliest match
        let match = null;
        let matchType = null;

        if (codeMatch && (!boldMatch || codeMatch.index < boldMatch.index) &&
            (!italicMatch || codeMatch.index < italicMatch.index) &&
            (!boldMatch || codeMatch.index < boldMatch.index)) {
            match = codeMatch;
            matchType = 'code';
        } else if (boldMatch && (!italicMatch || boldMatch.index < italicMatch.index)) {
            match = boldMatch;
            matchType = 'bold';
        } else if (italicMatch) {
            match = italicMatch;
            matchType = 'italic';
        }

        if (match && match.index > 0) {
            runs.push(new TextRun({
                text: remaining.substring(0, match.index)
            }));
            remaining = remaining.substring(match.index);
        }

        if (match) {
            const content = match[1];
            runs.push(new TextRun({
                text: content,
                ...(matchType === 'code' && { font: 'Consolas', size: 20 }),
                ...(matchType === 'bold' && { bold: true }),
                ...(matchType === 'italic' && { italics: true })
            }));
            remaining = remaining.substring(match[0].length);
        } else {
            runs.push(new TextRun({ text: remaining }));
            break;
        }
    }

    if (runs.length === 0) {
        runs.push(new TextRun({ text: text }));
    }

    return runs;
}

/**
 * Create a learning note styled as a callout
 */
function createLearningNote(text, isWhy) {
    return new Paragraph({
        children: [
            new TextRun({
                text: isWhy ? 'Why it matters: ' : 'Learning note: ',
                bold: true,
                color: COLORS.accent
            }),
            ...parseInlineFormatting(text)
        ],
        spacing: { before: 240, after: 240 },
        indent: { left: 720, hanging: 0 },
        border: {
            left: { color: COLORS.accent, size: 6, style: BorderStyle.SINGLE },
            top: { color: COLORS.accent, size: 1, style: BorderStyle.SINGLE },
            right: { color: COLORS.accent, size: 1, style: BorderStyle.SINGLE },
            bottom: { color: COLORS.accent, size: 1, style: BorderStyle.SINGLE }
        },
        shading: { fill: 'F0F7FF' }
    });
}

/**
 * Create a code block with syntax highlighting simulation
 */
function createCodeBlock(code, language) {
    const lines = code.split('\n');
    const paragraphs = [];

    for (const line of lines) {
        // Simple syntax highlighting simulation
        const runs = [];

        // Keywords to highlight
        const keywords = ['import', 'public', 'class', 'private', 'return', 'if', 'else',
            'new', 'this', 'throws', '@Override', '@Bean', '@Configuration'];

        let remaining = line;
        while (remaining.length > 0) {
            // Find keyword
            let keywordMatch = null;
            let keywordType = null;

            for (const kw of keywords) {
                const regex = new RegExp(`\\b${kw}\\b`);
                const match = remaining.match(regex);
                if (match && (!keywordMatch || match.index < keywordMatch.index)) {
                    keywordMatch = match;
                    keywordType = 'keyword';
                }
            }

            // Find strings
            const stringMatch = remaining.match(/"([^"]*)"/);
            if (stringMatch && (!keywordMatch || stringMatch.index < keywordMatch.index)) {
                keywordMatch = stringMatch;
                keywordType = 'string';
            }

            // Find comments
            const commentMatch = remaining.match(/\/\/.*$/);
            if (commentMatch && (!keywordMatch || commentMatch.index < keywordMatch.index)) {
                keywordMatch = commentMatch;
                keywordType = 'comment';
            }

            // Output text before match
            if (keywordMatch && keywordMatch.index > 0) {
                runs.push(new TextRun({
                    text: remaining.substring(0, keywordMatch.index),
                    font: 'Consolas',
                    size: 20
                }));
                remaining = remaining.substring(keywordMatch.index);
            }

            // Output match
            if (keywordMatch) {
                const isComment = keywordType === 'comment';
                const isString = keywordType === 'string';
                const isKeyword = keywordType === 'keyword';

                runs.push(new TextRun({
                    text: keywordMatch[0],
                    font: 'Consolas',
                    size: 20,
                    color: isComment ? '008000' : isString ? 'A31515' : isKeyword ? '0000FF' : '333333',
                    ...(isComment && { italics: true })
                }));
                remaining = remaining.substring(keywordMatch[0].length);
            } else if (remaining.length > 0) {
                runs.push(new TextRun({
                    text: remaining,
                    font: 'Consolas',
                    size: 20
                }));
                break;
            }
        }

        paragraphs.push(new Paragraph({
            children: runs,
            spacing: { line: 240 }
        }));
    }

    return new Table({
        rows: [
            new TableRow({
                children: [
                    new TableCell({
                        children: [
                            ...paragraphs,
                            language && new Paragraph({
                                children: [new TextRun({ text: `// ${language}`, color: '888888', size: 16 })],
                                alignment: AlignmentType.RIGHT
                            })
                        ],
                        shading: { fill: COLORS.codeBg },
                        margins: { top: 120, bottom: 120, left: 240, right: 240 }
                    })
                ]
            })
        ],
        width: { size: 100, type: WidthType.PERCENTAGE }
    });
}

/**
 * Create a chapter document
 */
function createChapterDocument(chapterNum, parsed, chapterFile) {
    const children = [];

    // Chapter number and title
    children.push(new Paragraph({
        children: [
            new TextRun({
                text: `Part ${chapterNum} - `,
                bold: true,
                size: 44,
                color: COLORS.accent
            }),
            new TextRun({
                text: parsed.h1.replace(/^Part \d+ - /, ''),
                bold: true,
                size: 44
            })
        ],
        spacing: { before: 480, after: 240 }
    }));

    // Sections
    for (const section of parsed.sections) {
        // Section heading based on level
        const headingLevel = section.level === 2 ? HeadingLevel.HEADING_1 :
            section.level === 3 ? HeadingLevel.HEADING_2 : HeadingLevel.HEADING_3;

        children.push(new Paragraph({
            children: [new TextRun({ text: section.title, bold: true, size: 28 })],
            heading: headingLevel,
            spacing: { before: 360, after: 120 }
        }));

        // Section content
        for (const item of section.content) {
            if (item.type === 'paragraph') {
                children.push(new Paragraph({
                    children: parseInlineFormatting(item.text),
                    spacing: { line: 360 }
                }));
            } else if (item.type === 'summary') {
                children.push(new Paragraph({
                    children: [
                        new TextRun({ text: 'Chapter summary: ', bold: true, color: COLORS.accent }),
                        ...parseInlineFormatting(item.text)
                    ],
                    spacing: { before: 240, after: 240 },
                    shading: { fill: 'FFFACD' }
                }));
            }
        }

        // Tables in this section
        for (const table of parsed.tables) {
            // Only add table if it appears in this section
            children.push(new Paragraph({ children: [], spacing: { before: 120 } }));
            children.push(createDocxTable(table));
        }
    }

    // Add code blocks (placed after their sections)
    for (const code of parsed.codeBlocks) {
        children.push(new Paragraph({ children: [], spacing: { before: 240 } }));
        children.push(createCodeBlock(code.content, code.lang));
    }

    // Add learning notes
    for (const note of parsed.learningNotes) {
        children.push(createLearningNote(note.text, note.type === 'why'));
    }

    return new Document({
        sections: [{
            properties: {},
            children
        }]
    });
}

/**
 * Generate the full document with cover page and TOC
 */
async function generateDocument() {
    const chapterOrder = [
        '01_front.md',
        '02_authentication.md',
        '03_password_storage.md',
        '04_authorization_roles.md',
        '05_store_isolation.md',
        '06_csrf_session.md',
        '07_hardening_gaps.md'
    ];

    const children = [];

    // ========== COVER PAGE ==========
    children.push(new Paragraph({
        children: [new TextRun({ text: CONFIG.title, bold: true, size: 60, color: COLORS.accent })],
        alignment: AlignmentType.CENTER,
        spacing: { after: 480 }
    }));

    children.push(new Paragraph({
        children: [new TextRun({ text: CONFIG.subtitle, size: 32, color: COLORS.muted })],
        alignment: AlignmentType.CENTER,
        spacing: { after: 720 }
    }));

    children.push(new Paragraph({ children: [], spacing: { before: 720 } }));

    children.push(new Paragraph({
        children: [new TextRun({ text: 'An Extremely Detailed Cybersecurity Learning Guide', italics: true, size: 24 })],
        alignment: AlignmentType.CENTER,
        spacing: { after: 1440 }
    }));

    children.push(new Paragraph({
        children: [new TextRun({ text: `Author: ${CONFIG.author}`, size: 20 })],
        alignment: AlignmentType.CENTER
    }));

    children.push(new Paragraph({
        children: [new TextRun({ text: `Generated: ${new Date().toLocaleDateString()}`, size: 20 })],
        alignment: AlignmentType.CENTER,
        spacing: { after: 720 }
    }));

    // Add a page break after cover
    children.push(new Paragraph({
        children: [new TextRun({ text: '', break: 1 })]
    }));

    // ========== TABLE OF CONTENTS ==========
    children.push(new Paragraph({
        children: [new TextRun({ text: 'Table of Contents', bold: true, size: 36 })],
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 720, after: 360 }
    }));

    let chapterNum = 1;
    for (const chapterFile of chapterOrder) {
        const filePath = path.join(CONFIG.chaptersDir, chapterFile);
        if (!fs.existsSync(filePath)) {
            console.log(`Warning: ${chapterFile} not found, skipping`);
            continue;
        }

        const parsed = parseMarkdown(filePath);
        const tocTitle = parsed.h1.replace(/^Part \d+ - /, '');

        children.push(new Paragraph({
            children: [
                new TextRun({ text: `${chapterNum}. `, size: 22 }),
                new TextRun({ text: tocTitle, size: 22 }),
                new TextRun({ text: ' '.repeat(60 - tocTitle.length) + ' ' + chapterNum, size: 18, color: COLORS.muted })
            ],
            spacing: { line: 360 }
        }));
        chapterNum++;
    }

    // Add page break
    children.push(new Paragraph({
        children: [new TextRun({ text: '', break: 1 })]
    }));

    // ========== CHAPTERS ==========
    chapterNum = 1;
    for (const chapterFile of chapterOrder) {
        const filePath = path.join(CONFIG.chaptersDir, chapterFile);
        if (!fs.existsSync(filePath)) {
            console.log(`Warning: ${chapterFile} not found, skipping`);
            continue;
        }

        console.log(`Processing chapter ${chapterNum}: ${chapterFile}`);
        const parsed = parseMarkdown(filePath);

        // Chapter title
        children.push(new Paragraph({
            children: [
                new TextRun({
                    text: `Part ${chapterNum} - `,
                    bold: true,
                    size: 36,
                    color: COLORS.accent
                }),
                new TextRun({
                    text: parsed.h1.replace(/^Part \d+ - /, ''),
                    bold: true,
                    size: 36
                })
            ],
            heading: HeadingLevel.HEADING_1,
            spacing: { before: 720, after: 360 }
        }));

        // Sections
        for (const section of parsed.sections) {
            // Section heading
            const headingLevel = section.level === 2 ? HeadingLevel.HEADING_2 :
                section.level === 3 ? HeadingLevel.HEADING_3 : HeadingLevel.HEADING_4;

            children.push(new Paragraph({
                children: [new TextRun({ text: section.title, bold: true, size: section.level === 2 ? 28 : 24 })],
                heading: headingLevel,
                spacing: { before: section.level === 2 ? 360 : 240, after: 120 }
            }));

            // Process content items
            for (const item of section.content) {
                if (item.type === 'paragraph') {
                    children.push(new Paragraph({
                        children: parseInlineFormatting(item.text),
                        spacing: { line: 360 }
                    }));
                } else if (item.type === 'summary') {
                    children.push(new Paragraph({
                        children: [
                            new TextRun({ text: 'Chapter summary: ', bold: true, color: COLORS.accent }),
                            ...parseInlineFormatting(item.text)
                        ],
                        spacing: { before: 240, after: 240 },
                        shading: { fill: 'FFFACD' }
                    }));
                }
            }
        }

        // Add tables for this chapter
        for (const table of parsed.tables) {
            children.push(new Paragraph({ children: [], spacing: { before: 240 } }));
            children.push(createDocxTable(table));
        }

        // Add code blocks
        for (const code of parsed.codeBlocks) {
            children.push(new Paragraph({ children: [], spacing: { before: 240 } }));
            children.push(createCodeBlock(code.content, code.lang));
        }

        // Add learning notes
        for (const note of parsed.learningNotes) {
            children.push(createLearningNote(note.text, note.type === 'why'));
        }

        // Add page break between chapters (except last)
        if (chapterNum < 7) {
            children.push(new Paragraph({
                children: [new TextRun({ text: '', break: 1 })]
            }));
        }

        chapterNum++;
    }

    // Create the final document
    const doc = new Document({
        sections: [{
            properties: {
                page: {
                    size: {
                        width: CONFIG.page.width,
                        height: CONFIG.page.height
                    },
                    margin: {
                        top: CONFIG.page.margin.top,
                        right: CONFIG.page.margin.right,
                        bottom: CONFIG.page.margin.bottom,
                        left: CONFIG.page.margin.left
                    }
                }
            },
            children
        }]
    });

    // Ensure output directory exists
    if (!fs.existsSync(CONFIG.outputDir)) {
        fs.mkdirSync(CONFIG.outputDir, { recursive: true });
    }

    // Pack and save
    const outputPath = path.join(CONFIG.outputDir, 'StoreMS_Security_Playbook.docx');
    const buffer = await Packer.toBuffer(doc);
    fs.writeFileSync(outputPath, buffer);

    console.log(`\nGenerated: ${outputPath}`);
    console.log(`File size: ${(buffer.length / 1024).toFixed(1)} KB`);

    return outputPath;
}

// Run if executed directly
if (require.main === module) {
    generateDocument()
        .then(() => {
            console.log('Done!');
            process.exit(0);
        })
        .catch(err => {
            console.error('Error:', err);
            process.exit(1);
        });
}

module.exports = { generateDocument, parseMarkdown, createDocxTable, createCodeBlock, createLearningNote };