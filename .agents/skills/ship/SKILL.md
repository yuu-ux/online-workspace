---
name: ship
description: このリポジトリでコード、テスト、設定、ドキュメントなどの変更作業を任されたときに使用する。読み取り専用の調査や回答には使用しない。Git worktree、ブランチ命名、push、Pull Request作成を一連で行う。
---

# リポジトリ作業フロー

1. 変更を始める前に、リポジトリルートの `docs/branch_naming_convention.md` を読む。
2. 命名規則に従うブランチを作成し、リポジトリルートの `.codex/worktrees/<branch-name>` に専用の Git worktree を作成する。
3. `main` ブランチでは直接作業せず、作成した worktree 内だけで変更する。
4. 必要な変更を実装し、変更内容に応じた最小限の検証を行う。
5. `git status` と `git diff` で、意図した変更だけが含まれることを確認する。
6. 変更をコミットし、作業ブランチをリモートへ push する。
7. Pull Request を作成し、そのURLと検証結果をユーザーへ報告する。
