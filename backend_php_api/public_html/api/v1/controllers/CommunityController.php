<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Middleware\AuthMiddleware;
use PDO;

/**
 * Community & Interactive Features Controller (`mind_game.php`, `studio.php`, `upload.php`, `help.php`, `footer.php`, `sufyan.php`)
 *
 * Implements:
 * - `GET /mind-game/questions` -> Returns 10 randomized quiz questions from `mind_game.php`
 * - `POST /studio/audition`    -> Handles authenticated Dubbing Studio voice/video upload (`upload.php` + `auditions` table)
 * - `GET /updates`             -> Returns platform updates (`sufyan.php`) & official social/help links (`help.php`, `footer.php`)
 */
final class CommunityController
{
    /**
     * Exact question bank from `mind_game.php` (`array_rand($all_questions, 10)`).
     */
    public static function mindGameQuestions(): never
    {
        $allQuestions = self::getQuestionBank();
        shuffle($allQuestions);
        $selected = array_slice($allQuestions, 0, 10);

        ResponseHelper::success([
            'totalQuestions' => count($selected),
            'questions'      => $selected,
        ]);
    }

    /**
     * Exact port of `upload.php` for `studio.php` Dubbing Auditions:
     * - Requires authenticated user (`AuthMiddleware::requireAuth($pdo)`)
     * - Enforces max 20 auditions/day (`SELECT COUNT(*) FROM auditions WHERE DATE(submitted_at) = CURDATE()`)
     * - Enforces max 10 MB file size (`10 * 1024 * 1024`)
     * - Validates audio/video extensions (`mp3, wav, ogg, m4a, aac, mp4, mkv, mov, webm, avi`)
     * - Inserts into `auditions (user_name, whatsapp, role, file_path)`
     */
    public static function submitAudition(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);

        // 1. Check daily limit of 20 auditions (`upload.php`)
        $stmt = $pdo->query("SELECT COUNT(*) AS total FROM auditions WHERE DATE(submitted_at) = CURDATE()");
        $todayCount = (int) ($stmt ? ($stmt->fetch()['total'] ?? 0) : 0);
        if ($todayCount >= 20) {
            ResponseHelper::error(
                'Daily audition limit (20/20) reached! Please try again tomorrow.',
                429,
                'DAILY_LIMIT_REACHED'
            );
        }

        $userName = trim((string) ($_POST['user_name'] ?? $_POST['name'] ?? ($auth['user']['name'] ?? '')));
        $whatsapp = trim((string) ($_POST['whatsapp'] ?? ''));
        $role = trim((string) ($_POST['role'] ?? 'Voice Actor'));

        if ($userName === '' || $whatsapp === '') {
            ResponseHelper::error('Your full name and WhatsApp number are required.', 422, 'VALIDATION_ERROR');
        }

        if (!isset($_FILES['voice_file']) || $_FILES['voice_file']['error'] !== UPLOAD_ERR_OK) {
            ResponseHelper::error('Please attach a valid audio or video sample file (max 10 MB).', 422, 'FILE_REQUIRED');
        }

        $fileSize = (int) $_FILES['voice_file']['size'];
        if ($fileSize > 10 * 1024 * 1024) {
            ResponseHelper::error('File is too large! Maximum allowed size is 10 MB.', 422, 'FILE_TOO_LARGE');
        }

        $originalName = basename((string) $_FILES['voice_file']['name']);
        $ext = strtolower((string) pathinfo($originalName, PATHINFO_EXTENSION));
        $allowedExts = ['mp3', 'wav', 'ogg', 'm4a', 'aac', 'mp4', 'mkv', 'mov', 'webm', 'avi'];

        if (!in_array($ext, $allowedExts, true)) {
            ResponseHelper::error('Invalid file format! Only audio and video files are allowed.', 422, 'INVALID_FILE_TYPE');
        }

        $uploadDir = dirname(__DIR__, 3) . '/uploads/';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0777, true);
        }

        $safeFileName = time() . '_' . preg_replace('/[^a-zA-Z0-9._-]/', '_', $originalName);
        $targetFilePath = $uploadDir . $safeFileName;
        $relativeDbPath = 'uploads/' . $safeFileName;

        if (!move_uploaded_file($_FILES['voice_file']['tmp_name'], $targetFilePath)) {
            ResponseHelper::error('Error saving uploaded audition file on server.', 500, 'UPLOAD_FAILED');
        }

        $insert = $pdo->prepare('
            INSERT INTO auditions (user_name, whatsapp, role, file_path)
            VALUES (:uname, :wa, :role, :fpath)
        ');
        $insert->execute([
            ':uname' => $userName,
            ':wa'    => $whatsapp,
            ':role'  => $role,
            ':fpath' => $relativeDbPath,
        ]);

        ResponseHelper::success([
            'auditionId'     => (int) $pdo->lastInsertId(),
            'userName'       => $userName,
            'whatsapp'       => $whatsapp,
            'role'           => $role,
            'filePath'       => $relativeDbPath,
            'remainingToday' => max(0, 20 - ($todayCount + 1)),
        ], 'Audition submitted successfully! Our team will contact you on WhatsApp if shortlisted.');
    }

    /**
     * Returns platform updates (`sufyan.php`) and official support/social links (`help.php`, `footer.php`).
     */
    public static function updates(): never
    {
        ResponseHelper::success([
            'updates' => [
                [
                    'id'          => 'update_dubbing_studio',
                    'badge'       => 'New Feature',
                    'title'       => 'HaniX Urdu Dubbing Studio Auditions Open',
                    'date'        => 'Latest Update',
                    'description' => 'Submit your 30-second Urdu voice acting or video editing sample directly from the Dubbing Studio tab. Up to 20 auditions accepted daily.',
                ],
                [
                    'id'          => 'update_mind_games',
                    'badge'       => 'Interactive',
                    'title'       => 'Mind Games Quiz & Brain Teasers',
                    'date'        => 'Active Now',
                    'description' => 'Challenge yourself with 10 randomized Urdu/English general knowledge and movie trivia questions in every round.',
                ],
                [
                    'id'          => 'update_multi_server',
                    'badge'       => 'Streaming',
                    'title'       => 'Dual High-Speed Streaming Servers + Urdu Subtitles',
                    'date'        => 'Platform Upgrade',
                    'description' => 'Switch seamlessly between Main Server and Server 2 during playback, and browse our dedicated 16:9 Urdu Subtitles collection.',
                ],
            ],
            'support' => [
                'whatsappSupportUrl' => 'https://wa.me/923160450074',
                'whatsappChannelUrl' => 'https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q',
                'youtubeUrl'         => 'https://youtube.com/@Hanixurdu?si=c9-0aD5e5f_P2AC-',
                'instagramUrl'       => 'https://www.instagram.com/hanixurdu?igsh=MW54Z3k2OTR6b3B4cA==',
                'facebookUrl'        => 'https://www.facebook.com/share/1779797B6C/',
                'tiktokUrl'          => 'https://www.tiktok.com/@hanixurdu',
                'supportEmail'       => 'hannnix88@gmail.com',
                'jazzCashName'       => 'Muhammad Hasnain',
                'jazzCashNumber'     => '03294806457',
            ],
        ]);
    }

    private static function getQuestionBank(): array
    {
        return [
            ['q' => 'What is the capital of Pakistan?', 'options' => ['Lahore', 'Karachi', 'Islamabad', 'Peshawar'], 'answer' => 2],
            ['q' => 'Which planet is known as the Red Planet?', 'options' => ['Venus', 'Mars', 'Jupiter', 'Saturn'], 'answer' => 1],
            ['q' => 'How many continents are there in the world?', 'options' => ['5', '6', '7', '8'], 'answer' => 2],
            ['q' => 'What is the largest mammal in the world?', 'options' => ['Elephant', 'Blue Whale', 'Giraffe', 'Shark'], 'answer' => 1],
            ['q' => 'Who wrote the national anthem of Pakistan?', 'options' => ['Allama Iqbal', 'Hafeez Jalandhari', 'Faiz Ahmed Faiz', 'Mirza Ghalib'], 'answer' => 1],
            ['q' => 'Which gas do plants absorb from the atmosphere?', 'options' => ['Oxygen', 'Nitrogen', 'Carbon Dioxide', 'Hydrogen'], 'answer' => 2],
            ['q' => 'What is the boiling point of water?', 'options' => ['50°C', '90°C', '100°C', '120°C'], 'answer' => 2],
            ['q' => 'Which is the longest river in the world?', 'options' => ['Amazon', 'Nile', 'Indus', 'Yangtze'], 'answer' => 1],
            ['q' => 'How many players are there in a cricket team?', 'options' => ['9', '10', '11', '12'], 'answer' => 2],
            ['q' => 'Which element has the chemical symbol O?', 'options' => ['Gold', 'Silver', 'Oxygen', 'Iron'], 'answer' => 2],
            ['q' => 'What is the national flower of Pakistan?', 'options' => ['Rose', 'Jasmine', 'Sunflower', 'Tulip'], 'answer' => 1],
            ['q' => 'Which ocean is the largest in the world?', 'options' => ['Atlantic', 'Indian', 'Arctic', 'Pacific'], 'answer' => 3],
            ['q' => 'Who invented the light bulb?', 'options' => ['Nikola Tesla', 'Thomas Edison', 'Alexander Bell', 'Isaac Newton'], 'answer' => 1],
            ['q' => 'Which country is home to the Kangaroo?', 'options' => ['Canada', 'Brazil', 'Australia', 'South Africa'], 'answer' => 2],
            ['q' => 'How many bones are in the adult human body?', 'options' => ['196', '206', '216', '226'], 'answer' => 1],
            ['q' => 'Which is the highest mountain in the world?', 'options' => ['K2', 'Mount Everest', 'Nanga Parbat', 'Kilimanjaro'], 'answer' => 1],
            ['q' => 'What is the currency of Japan?', 'options' => ['Yuan', 'Won', 'Yen', 'Ringgit'], 'answer' => 2],
            ['q' => 'Which organ pumps blood in the human body?', 'options' => ['Brain', 'Lungs', 'Heart', 'Liver'], 'answer' => 2],
            ['q' => 'What is the speed of light approximately?', 'options' => ['300,000 km/s', '150,000 km/s', '1,000 km/s', '30,000 km/s'], 'answer' => 0],
            ['q' => 'Which language has the most native speakers?', 'options' => ['English', 'Spanish', 'Mandarin Chinese', 'Hindi'], 'answer' => 2],
        ];
    }
}
